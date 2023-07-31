package ru.joke.memcache.core.internal;

import ru.joke.memcache.core.ManagedCache;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;
import ru.joke.memcache.core.events.*;
import ru.joke.memcache.core.internal.util.CompositeCollection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

@ThreadSafe
final class MapMemCache<K extends Serializable, V extends Serializable> implements MemCache<K, V>, ManagedCache {

    private final CacheConfiguration configuration;
    private final AsyncOpsInvoker asyncOpsInvoker;
    private final EntryMetadataFactory entryMetadataFactory;
    private final Set<EntryMetadata<?, K>> entriesMetadata;
    private final List<CacheEntryEventListener<K, V>> listeners;
    private final ThreadLocal<MemCacheEntry<K, V>> oldEntryContainer;
    private final boolean eternal;
    private final PersistentCacheRepository persistentCacheRepository;
    private volatile Map<K, MemCacheEntry<K, V>>[] segments;

    MapMemCache(@Nonnull CacheConfiguration configuration,
                @Nonnull AsyncOpsInvoker asyncOpsInvoker,
                @Nonnull PersistentCacheRepository persistentCacheRepository,
                @Nonnull EntryMetadataFactory metadataFactory) {
        this.configuration = configuration;
        this.asyncOpsInvoker = asyncOpsInvoker;
        this.oldEntryContainer = new ThreadLocal<>();
        this.persistentCacheRepository = persistentCacheRepository;
        this.eternal = configuration.expirationConfiguration().eternal();
        this.segments = createSegments();
        this.listeners = new CopyOnWriteArrayList<>(configuration.registeredEventListeners());
        this.entryMetadataFactory = metadataFactory;

        final long maxEntries = configuration.memoryStoreConfiguration().maxEntries();
        this.entriesMetadata = new ConcurrentSkipListSet<>() {
            @Override
            public boolean add(EntryMetadata<?, K> metadata) {
                final boolean overflow = size() >= maxEntries;

                if (overflow) {
                    if (contains(metadata)) {
                        return false;
                    }

                    // Removing by remove(..) may be run concurrent - its normal
                    synchronized (this) {

                        // Firstly we will try to remove expired entries if possible
                        if (!eternal && size() >= maxEntries) {
                            clearExpired();
                        }

                        while (size() >= maxEntries) {

                            final EntryMetadata<?, K> removedEntry = first();
                            if (removedEntry != null) {
                                MapMemCache.this.remove(removedEntry.key());
                            }
                        }

                        return super.add(metadata);
                    }
                }

                return super.add(metadata);
            }
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return this.configuration.cacheName();
    }

    @Nonnull
    @Override
    public CacheConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public boolean registerEventListener(@Nonnull CacheEntryEventListener<K, V> listener) {
        return this.listeners.add(listener);
    }

    @Override
    public boolean deregisterEventListener(@Nonnull CacheEntryEventListener<K, V> listener) {
        return this.listeners.removeIf(l -> l.equals(listener));
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> entry = segment.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        entry.metadata().onUsage();

        return Optional.of(entry.value());
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        return computeIfPresent(key, (k, v) -> null, true);
    }

    @Override
    public boolean remove(@Nonnull K key, @Nonnull V value) {
        return replace(key, value, null);
    }

    @Nonnull
    @Override
    public Optional<V> put(@Nonnull final K key, @Nullable final V value) {
        return compute(key, (k, v) -> value, true, false);
    }

    @Override
    public Optional<V> putIfAbsent(@Nonnull K key, @Nullable V value) {
        return compute(key, (k, v) -> v == null ? value : v, true, false);
    }

    @Override
    public void clear() {
        final Map<K, MemCacheEntry<K, V>>[] newSegments = createSegments();
        // not atomic, but not terrible for entriesMetadata; floating entries (i.e. trash) added between next two constructions will be removed eventually
        this.entriesMetadata.clear();
        this.segments = newSegments;

        final CacheEntriesEvent<K, V> clearEvent = new DefaultCacheEntriesEvent<>(EventType.REMOVED, this);
        this.listeners.forEach(l -> l.onBatchEvent(clearEvent));
    }

    @Override
    public Optional<V> merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {
        return this.compute(key, (k, oldV) -> oldV == null ? value : mergeFunction.apply(oldV, value));
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {

        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> newEntry = segment.computeIfAbsent(
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

        if (newEntry == null || this.oldEntryContainer.get() == null) {
            return Optional.empty();
        }

        this.oldEntryContainer.remove();

        final Optional<V> newValue = Optional.of(newEntry.value());
        final EventType eventType = EventType.ADDED;
        final var event = new DefaultCacheEntryEvent<>(key, Optional.empty(), newValue, eventType, this);
        this.listeners.forEach(l -> l.onEvent(event));

        return newValue;
    }

    @Nonnull
    @Override
    public Optional<V> compute(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return compute(key, remappingFunction, false, false);
    }

    @Nonnull
    @Override
    public Optional<V> computeIfPresent(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(key, remappingFunction, false);
    }

    @Override
    public boolean replace(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue) {

        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> newEntry = segment.compute(
                key,
                (k, v) -> {
                    this.oldEntryContainer.set(v);

                    if (v == null && oldValue != null || v != null && oldValue != v.value()) {
                        return v;
                    }

                    if (newValue == null && v == null) {
                        return null;
                    } else if (newValue == null) {
                        this.entriesMetadata.remove(v.metadata());
                        return null;
                    }

                    final MemCacheEntry<K, V> result = new MemCacheEntry<>(
                            newValue,
                            v == null ? this.entryMetadataFactory.create(k) : v.metadata()
                    );

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
        // TODO state transition and checks

        restoreFromRepository();
    }

    @Override
    public synchronized void shutdown() {
        // TODO state transition and checks

        persistToRepository();
    }

    boolean eternal() {
        return this.eternal;
    }

    void clearExpired() {
        final long idleExpirationTimeout = this.configuration.expirationConfiguration().idleTimeout();
        final long idleExpirationTime = System.currentTimeMillis() - idleExpirationTimeout;
        this.entriesMetadata.forEach(metadata -> {
            if (metadata.expiredByLifespanAt() <= System.currentTimeMillis() || metadata.lastAccessed() <= idleExpirationTime) {
                // eviction of element from cache data
                compute(metadata.key(), (k, v) -> {
                    this.entriesMetadata.remove(metadata);
                    return null;
                }, true, true);
            }
        });
    }

    private void restoreFromRepository() {
        final Collection<MemCacheEntry<K, V>> restoredEntries = this.persistentCacheRepository.load();

        restoredEntries.forEach(entry -> {
            final K key = entry.metadata().key();
            this.computeSegment(key).put(key, entry);
            this.entriesMetadata.add(entry.metadata());
        });
    }

    private void persistToRepository() {
        final CompositeCollection<MemCacheEntry<K, V>> compositeCollection = new CompositeCollection<>(this.segments.length);
        for (final Map<K, MemCacheEntry<K, V>> segment : this.segments) {
            compositeCollection.addCollection(segment.values());
        }

        this.persistentCacheRepository.save(compositeCollection);
    }

    private Optional<V> computeIfPresent(
            @Nonnull K key,
            @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            boolean returnOldValue) {
        final Map<K, MemCacheEntry<K, V>> segment = computeSegment(key);
        final MemCacheEntry<K, V> newEntry = segment.computeIfPresent(
                key,
                (k, v) -> {
                    final V newVal = remappingFunction.apply(k, v.value());
                    this.oldEntryContainer.set(v);

                    if (newVal == null) {
                        this.entriesMetadata.remove(v.metadata());
                        return null;
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

            return returnOldValue ? oldValue : newValue;
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
            return Optional.empty();
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
        final int segmentsCount = concurrencyLevel % 2 == 1 ? concurrencyLevel + 1 : concurrencyLevel;

        @SuppressWarnings("unchecked")
        final Map<K, MemCacheEntry<K, V>>[] segments = new ConcurrentHashMap[segmentsCount];
        for (int i = 0; i < segmentsCount; i++) {
            segments[i] = new ConcurrentHashMap<>(storeConfiguration.maxEntries() / segmentsCount);
        }

        return segments;
    }
}
