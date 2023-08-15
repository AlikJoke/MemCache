package ru.joke.memcache.core.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.joke.memcache.core.LifecycleException;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.MemCacheException;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;
import ru.joke.memcache.core.events.*;
import ru.joke.memcache.core.internal.util.CompositeCollection;
import ru.joke.memcache.core.stats.MemCacheStatistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

@ThreadSafe
final class MapMemCache<K extends Serializable, V extends Serializable> implements MemCache<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(MapMemCache.class);
    private static final int ALLOWED_OVERFLOW_NO_LOCK = 1000;

    private final CacheConfiguration configuration;
    private final AsyncOpsInvoker asyncOpsInvoker;
    private final EntryMetadataFactory entryMetadataFactory;
    private final ConcurrentSkipListSet<EntryMetadata<?, K>> entriesMetadata;
    private final List<CacheEntryEventListener<K, V>> listeners;
    private final ThreadLocal<MemCacheEntry<K, V>> oldEntryContainer;
    private final AtomicInteger metadataCounter;
    private final boolean eternal;
    private final int maxEntries;
    private final PersistentCacheRepository persistentCacheRepository;
    private final InternalMemCacheStatistics statistics;

    private volatile long nearestElementExpirationTime;
    private volatile ComponentStatus status;
    private volatile Map<K, MemCacheEntry<K, V>>[] segments;

    MapMemCache(@Nonnull CacheConfiguration configuration,
                @Nonnull AsyncOpsInvoker asyncOpsInvoker,
                @Nonnull PersistentCacheRepository persistentCacheRepository,
                @Nonnull EntryMetadataFactory metadataFactory) {
        this.status = ComponentStatus.UNAVAILABLE;
        this.configuration = configuration;
        this.maxEntries = configuration().memoryStoreConfiguration().maxEntries();
        this.metadataCounter = new AtomicInteger(0);
        this.asyncOpsInvoker = asyncOpsInvoker;
        this.oldEntryContainer = new ThreadLocal<>();
        this.persistentCacheRepository = persistentCacheRepository;
        this.eternal = configuration.expirationConfiguration().eternal();
        this.segments = createSegments();
        this.listeners = new CopyOnWriteArrayList<>(configuration.eventListeners());
        this.entryMetadataFactory = metadataFactory;
        this.statistics = new InternalMemCacheStatistics(() -> {
            final var segments = this.segments;
            int count = 0;
            for (var segment : segments) {
                count += segment.size();
            }

            return count;
        });

        final int maxEntries = configuration.memoryStoreConfiguration().maxEntries();
        this.entriesMetadata = new ConcurrentSkipListSet<>() {
            @Override
            public boolean add(EntryMetadata<?, K> metadata) {
                if (super.add(metadata)) {
                    metadataCounter.incrementAndGet();
                    return true;
                }

                return false;
            }

            @Override
            public boolean remove(Object o) {
                if (super.remove(o)) {
                    metadataCounter.decrementAndGet();
                    return true;
                }

                return false;
            }
        };
    }

    @Nonnull
    @Override
    public String name() {
        return this.configuration.cacheName();
    }

    @Nonnull
    @Override
    public CacheConfiguration configuration() {
        return this.configuration;
    }

    @Override
    public boolean registerEventListener(@Nonnull CacheEntryEventListener<K, V> listener) {
        if (this.status != ComponentStatus.RUNNING && this.status != ComponentStatus.INITIALIZING) {
            throw new LifecycleException("Event listener registration available only in " + ComponentStatus.RUNNING + " or " + ComponentStatus.INITIALIZING + " state; current state is " + this.status);
        }

        logger.debug("Event listener registration was called {} for cache {}", listener, this);
        return this.listeners.add(listener);
    }

    @Override
    public boolean deregisterEventListener(@Nonnull CacheEntryEventListener<K, V> listener) {
        if (this.status != ComponentStatus.RUNNING && this.status != ComponentStatus.INITIALIZING) {
            throw new LifecycleException("Event listener deregistration available only in " + ComponentStatus.RUNNING + " or " + ComponentStatus.INITIALIZING + " state; current state is " + this.status);
        }

        logger.debug("Event listener will be unregistered {} for cache {}", listener, this);
        return this.listeners.removeIf(l -> l.equals(listener));
    }

    @Nonnull
    @Override
    public MemCacheStatistics statistics() {
        return this.statistics;
    }

    @Nonnull
    @Override
    public ComponentStatus status() {
        return this.status;
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> entry = segment.get(key);
        if (entry == null) {
            this.statistics.onReadOnlyRetrievalMiss();
            return Optional.empty();
        }

        entry.metadata().onUsage();
        this.statistics.onReadOnlyRetrievalHit();

        return Optional.of(entry.value());
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        final var result = computeIfPresent(key, (k, v) -> null, true);
        result.ifPresentOrElse(v -> this.statistics.onRemovalHit(), this.statistics::onRemovalMiss);

        return result;
    }

    @Override
    public boolean remove(@Nonnull K key, @Nonnull V value) {
        return replace(key, value, null);
    }

    @Nonnull
    @Override
    public Optional<V> put(@Nonnull final K key, @Nullable final V value) {
        final var oldValue = compute(key, (k, v) -> value, true, false);
        oldValue.ifPresentOrElse(
                v -> {
                    if (value == null) {
                        this.statistics.onRemovalHit();
                    } else {
                        this.statistics.onPutHit();
                    }
                },
                () -> {
                    if (value != null) {
                        this.statistics.onPutHit();
                    }
                }
        );

        return oldValue;
    }

    @Override
    public Optional<V> putIfAbsent(@Nonnull K key, @Nullable V value) {
        final var oldValue = compute(key, (k, v) -> v == null ? value : v, true, false);
        oldValue.ifPresentOrElse(
                v -> this.statistics.onPutMiss(),
                () -> {
                    if (value != null) {
                        this.statistics.onPutHit();
                    }
                }
        );

        return oldValue;
    }

    @Override
    public void clear() {
        logger.debug("Cache cleaning was called: {}", this);

        final Map<K, MemCacheEntry<K, V>>[] newSegments = createSegments();
        // not atomic, but not terrible for entriesMetadata; floating entries (i.e. trash) added between next two constructions will be removed eventually
        this.entriesMetadata.clear();
        this.segments = newSegments;

        final CacheEntriesEvent<K, V> clearEvent = new DefaultCacheEntriesEvent<>(EventType.REMOVED, this);
        this.listeners.forEach(l -> l.onBatchEvent(clearEvent));
    }

    @Override
    @Nonnull
    public Optional<V> merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {
        return this.compute(key, (k, oldV) -> {
            if (oldV == null) {
                this.statistics.onPutHit();
                return value;
            } else {
                final V newVal = mergeFunction.apply(oldV, value);
                if (newVal == null) {
                    this.statistics.onRemovalHit();
                } else if (!oldV.equals(newVal)) {
                    this.statistics.onPutHit();
                } else {
                    this.statistics.onPutMiss();
                }

                return newVal;
            }
        }, false, false);
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {

        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> resultEntry = segment.computeIfAbsent(
                key,
                k -> {
                    final V value = valueFunction.apply(k);
                    if (value == null) {
                        return null;
                    }

                    final MemCacheEntry<K, V> result = new MemCacheEntry<>(
                            value,
                            this.entryMetadataFactory.create(k)
                    );
                    this.oldEntryContainer.set(result);

                    this.entriesMetadata.add(result.metadata());
                    return result;
                }
        );

        final boolean valueComputed = this.oldEntryContainer.get() != null;
        this.oldEntryContainer.remove();

        if (resultEntry == null) {
            return Optional.empty();
        }

        if (!valueComputed) {
            this.statistics.onReadOnlyRetrievalHit();
            return Optional.of(resultEntry.value());
        }

        clearEntriesByEvictionPolicyIfOverflow();

        this.statistics.onPutHit();

        final Optional<V> newValue = Optional.of(resultEntry.value());
        final EventType eventType = EventType.ADDED;
        final var event = new DefaultCacheEntryEvent<>(key, Optional.empty(), newValue, eventType, this);
        this.listeners.forEach(l -> l.onEvent(event));

        return newValue;
    }

    @Nonnull
    @Override
    public Optional<V> compute(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return compute(key, (k, v) -> {
            final var newVal = remappingFunction.apply(k, v);
            if (newVal == null && v != null) {
                this.statistics.onRemovalHit();
            } else if (v == null && newVal != null || v != null && !v.equals(newVal)) {
                this.statistics.onPutHit();
            }

            return newVal;
        }, false, false);
    }

    @Nonnull
    @Override
    public Optional<V> computeIfPresent(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(key, (k, v) -> {
            final var newVal = remappingFunction.apply(k, v);
            if (newVal == null) {
                this.statistics.onRemovalHit();
            } else if (!v.equals(newVal)) {
                this.statistics.onPutHit();
            }

            return newVal;
        }, false);
    }

    @Override
    public boolean replace(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue) {

        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> newEntry = segment.compute(
                key,
                (k, v) -> {
                    this.oldEntryContainer.set(v);

                    if (v == null && oldValue != null || v != null && !v.value().equals(oldValue)) {
                        if (newValue != null) {
                            this.statistics.onPutMiss();
                        } else {
                            this.statistics.onRemovalMiss();
                        }

                        return v;
                    }

                    if (newValue == null && v == null) {
                        return null;
                    } else if (newValue == null) {
                        this.statistics.onRemovalHit();
                        this.entriesMetadata.remove(v.metadata());
                        return null;
                    }

                    final MemCacheEntry<K, V> result = new MemCacheEntry<>(
                            newValue,
                            v == null ? this.entryMetadataFactory.create(k) : v.metadata()
                    );

                    this.statistics.onPutHit();
                    if (v == null) {
                        this.entriesMetadata.add(result.metadata());
                    }

                    return result;
                }
        );

        final MemCacheEntry<K, V> oldEntry = this.oldEntryContainer.get();
        if (newEntry == oldEntry) {
            this.oldEntryContainer.remove();
            return false;
        }

        try {
            final EventType eventType = oldValue == null
                                            ? EventType.ADDED
                                            : newValue == null
                                                ? EventType.REMOVED
                                                : EventType.UPDATED;
            if (eventType == EventType.ADDED) {
                clearEntriesByEvictionPolicyIfOverflow();
            }

            final var event = new DefaultCacheEntryEvent<>(key, oldValue, newValue, eventType, this);
            this.listeners.forEach(l -> l.onEvent(event));

            return true;
        } finally {
            this.oldEntryContainer.remove();
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> getAsync(@Nonnull K key) {
        return this.asyncOpsInvoker.invoke(() -> get(key));
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> removeAsync(@Nonnull K key) {
        return this.asyncOpsInvoker.invoke(() -> remove(key));
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> removeAsync(@Nonnull K key, @Nonnull V value) {
        return this.asyncOpsInvoker.invoke(() -> remove(key, value));
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> putAsync(@Nonnull K key, @Nullable V value) {
        return this.asyncOpsInvoker.invoke(() -> put(key, value));
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> putIfAbsentAsync(@Nonnull K key, @Nullable V value) {
        return this.asyncOpsInvoker.invoke(() -> putIfAbsent(key, value));
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> clearAsync() {
        return this.asyncOpsInvoker.invoke(this::clear);
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> mergeAsync(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {
        return this.asyncOpsInvoker.invoke(() -> merge(key, value, mergeFunction));
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> computeIfAbsentAsync(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {
        return this.asyncOpsInvoker.invoke(() -> computeIfAbsent(key, valueFunction));
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> computeAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return this.asyncOpsInvoker.invoke(() -> compute(key, remappingFunction));
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<V>> computeIfPresentAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return this.asyncOpsInvoker.invoke(() -> computeIfPresent(key, remappingFunction));
    }

    @Nonnull
    @Override
    public CompletableFuture<Boolean> replaceAsync(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue) {
        return this.asyncOpsInvoker.invoke(() -> replace(key, oldValue, newValue));
    }

    @Override
    public synchronized void initialize() {
        if (this.status != ComponentStatus.UNAVAILABLE) {
            throw new LifecycleException("Cache initialization available only in " + ComponentStatus.UNAVAILABLE + " state. Current state is " + this.status);
        }

        logger.info("Cache {} initialization was called", this);

        this.status = ComponentStatus.INITIALIZING;

        try {
            restoreFromRepository();
        } catch (RuntimeException ex) {
            this.status = ComponentStatus.FAILED;
            logger.error("Unable to restore data from disk for cache " + this, ex);
            throw (ex instanceof MemCacheException ? ex : new MemCacheException(ex));
        }

        this.status = ComponentStatus.RUNNING;

        logger.info("Cache {} initialization was completed", this);
    }

    @Override
    public synchronized void shutdown() {
        if (this.status != ComponentStatus.RUNNING) {
            throw new LifecycleException("Cache shutdown available only in " + ComponentStatus.RUNNING + " state. Current state is " + this.status);
        }

        logger.info("Cache {} shutdown was called", this);

        this.status = ComponentStatus.STOPPING;

        try {
            persistToRepository();
        } catch (RuntimeException ex) {
            this.status = ComponentStatus.FAILED;
            logger.error("Unable to persist data to disk for cache " + this, ex);
            throw (ex instanceof MemCacheException ? ex : new MemCacheException(ex));
        }

        this.status = ComponentStatus.TERMINATED;

        logger.info("Cache {} shutdown was completed", this);
    }

    @Override
    public String toString() {
        return "MemCache{" +
                "name=" + name() +
                ", eternal=" + eternal +
                ", status=" + status +
                '}';
    }

    boolean eternal() {
        return this.eternal;
    }

    void clearEntriesByEvictionPolicyIfOverflow() {
        final boolean overflow = this.metadataCounter.get() > this.maxEntries;

        if (overflow) {

            // Firstly we will try to remove expired entries if possible
            if (!this.eternal && this.metadataCounter.get() > this.maxEntries) {
                clearExpired();
            }

            while (this.metadataCounter.get() > this.maxEntries) {

                final EntryMetadata<?, K> removedEntry = this.entriesMetadata.first();
                if (removedEntry != null) {
                    computeIfPresent(
                            removedEntry.key(),
                            (k, v) -> {
                                this.statistics.onEviction();
                                return null;
                            },
                            true
                    );
                }
            }
        }
    }

    void clearExpired() {
        logger.trace("Expired entries cleaning was called: {}", this);

        if (this.status != ComponentStatus.RUNNING) {
            return;
        }

        final LongContainer currentTime = new LongContainer();
        currentTime.value = System.currentTimeMillis();
        if (this.nearestElementExpirationTime > currentTime.value) {
            return;
        }

        synchronized (this) {

            if (this.nearestElementExpirationTime > (currentTime.value = System.currentTimeMillis())) {
                return;
            }

            final long idleExpirationTimeout = this.configuration.expirationConfiguration().idleTimeout();
            final long idleExpirationTime = idleExpirationTimeout < 0 ? idleExpirationTimeout : currentTime.value - idleExpirationTimeout;

            final LongContainer nearestElementExpirationInterval = new LongContainer();
            nearestElementExpirationInterval.value = idleExpirationTimeout;

            this.entriesMetadata.forEach(metadata -> {
                final long expiredByLifespanAfter = metadata.expiredByLifespanAt() - currentTime.value;
                final long expiredByIdleTimeoutAfter = metadata.lastAccessed() - idleExpirationTime;
                if (expiredByLifespanAfter <= 0 || expiredByIdleTimeoutAfter <= 0) {
                    // eviction of element from cache data
                    compute(metadata.key(), (k, v) -> {
                        this.statistics.onExpiration();
                        this.entriesMetadata.remove(metadata);
                        return null;
                    }, true, true);
                } else if (nearestElementExpirationInterval.value > expiredByLifespanAfter || nearestElementExpirationInterval.value > expiredByIdleTimeoutAfter) {
                    nearestElementExpirationInterval.value = Math.min(expiredByLifespanAfter, expiredByIdleTimeoutAfter);
                }
            });

            this.nearestElementExpirationTime = currentTime.value + nearestElementExpirationInterval.value;
        }

        logger.trace("Expired entries cleaning was completed: {}", this);
    }

    private void restoreFromRepository() {
        logger.debug("Restore from disk was called: {}", this);

        final Collection<MemCacheEntry<K, V>> restoredEntries = this.persistentCacheRepository.load();

        restoredEntries.forEach(entry -> {
            final K key = entry.metadata().key();
            this.computeSegment(key).put(key, entry);
            this.entriesMetadata.add(entry.metadata());
        });

        clearEntriesByEvictionPolicyIfOverflow();

        logger.debug("Restore from disk was completed (entries {}): {}", restoredEntries.size(), this);
    }

    private void persistToRepository() {
        logger.debug("Persist to disk was called: {}", this);

        final CompositeCollection<MemCacheEntry<K, V>> compositeCollection = new CompositeCollection<>(this.segments.length);
        for (final Map<K, MemCacheEntry<K, V>> segment : this.segments) {
            compositeCollection.addCollection(segment.values());
        }

        logger.debug("Persist to disk will be executed for cache {} and {} entries", this, compositeCollection.size());

        this.persistentCacheRepository.save(compositeCollection);
    }

    private Optional<V> computeIfPresent(
            @Nonnull K key,
            @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            boolean forRemoval) {
        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> newEntry = segment.computeIfPresent(
                key,
                (k, v) -> {
                    final V newVal = remappingFunction.apply(k, v.value());
                    this.oldEntryContainer.set(v);

                    if (newVal == null) {
                        this.entriesMetadata.remove(v.metadata());
                        return null;
                    } else if (newVal.equals(v.value())) {
                        return v;
                    }

                    return new MemCacheEntry<>(newVal, v.metadata());
                }
        );

        final MemCacheEntry<K, V> oldEntry = this.oldEntryContainer.get();
        if (oldEntry == null) {
            return Optional.empty();
        }

        final Optional<V> oldValue = Optional.of(oldEntry.value());
        final Optional<V> newValue = newEntry == null ? Optional.empty() : Optional.of(newEntry.value());
        try {
            final EventType eventType = newValue.isEmpty()
                                            ? EventType.REMOVED
                                            : EventType.UPDATED;
            final var event = new DefaultCacheEntryEvent<>(key, oldValue, newValue, eventType, this);
            this.listeners.forEach(l -> l.onEvent(event));

            return forRemoval ? oldValue : newValue;
        } finally {
            this.oldEntryContainer.remove();
        }
    }

    private Optional<V> compute(
            @Nonnull K key,
            @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            boolean returnOldValue,
            boolean ifEntryRemovedPopulateExpirationEvent) {

        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> newEntry = segment.compute(
                key,
                (k, v) -> {
                    this.oldEntryContainer.set(v);

                    final V newVal = remappingFunction.apply(k, v == null ? null : v.value());
                    if (newVal == null && v == null) {
                        return null;
                    } else if (newVal == null) {
                        this.entriesMetadata.remove(v.metadata());
                        return null;
                    } else if (v != null && v.value().equals(newVal)) {
                        return v;
                    }

                    final MemCacheEntry<K, V> result = new MemCacheEntry<>(
                            newVal,
                            v == null ? this.entryMetadataFactory.create(k) : v.metadata()
                    );

                    if (v == null) {
                        this.entriesMetadata.add(result.metadata());
                    }

                    return result;
                }
        );

        final MemCacheEntry<K, V> oldEntry = this.oldEntryContainer.get();
        if (newEntry == null && oldEntry == null) {
            return Optional.empty();
        } else if (newEntry == oldEntry) {
            this.oldEntryContainer.remove();
            return Optional.of(oldEntry.value());
        }

        final Optional<V> oldValue = oldEntry == null ? Optional.empty() : Optional.of(oldEntry.value());
        final Optional<V> newValue = newEntry == null ? Optional.empty() : Optional.of(newEntry.value());
        try {
            final EventType eventType = oldValue.isEmpty()
                                            ? EventType.ADDED
                                            : newValue.isEmpty()
                                                ? ifEntryRemovedPopulateExpirationEvent
                                                    ? EventType.EXPIRED
                                                    : EventType.REMOVED
                                                : EventType.UPDATED;

            if (eventType == EventType.ADDED) {
                clearEntriesByEvictionPolicyIfOverflow();
            }

            final var event = new DefaultCacheEntryEvent<>(key, oldValue, newValue, eventType, this);
            this.listeners.forEach(l -> l.onEvent(event));

            return returnOldValue ? oldValue : newValue;
        } finally {
            this.oldEntryContainer.remove();
        }
    }

    private Map<K, MemCacheEntry<K, V>> computeSegment(final K key) {
        final int keyHash = key.hashCode();
        final int hash = keyHash ^ (keyHash >>> 16);
        return this.segments[this.segments.length - 1 & hash];
    }

    private Map<K, MemCacheEntry<K, V>>[] createSegments() {
        final MemoryStoreConfiguration storeConfiguration = configuration.memoryStoreConfiguration();
        final int concurrencyLevel = storeConfiguration.concurrencyLevel();
        int segmentsCount = concurrencyLevel % 2 == 1 ? concurrencyLevel + 1 : concurrencyLevel;
        while (segmentsCount > 1 && storeConfiguration.maxEntries() / segmentsCount < 2) {
            segmentsCount /= 2;
        }

        @SuppressWarnings("unchecked")
        final Map<K, MemCacheEntry<K, V>>[] segments = new ConcurrentHashMap[segmentsCount];
        for (int i = 0; i < segmentsCount; i++) {
            segments[i] = new ConcurrentHashMap<>(storeConfiguration.maxEntries() / segmentsCount);
        }

        return segments;
    }

    private static class LongContainer {
        private long value;
    }
}
