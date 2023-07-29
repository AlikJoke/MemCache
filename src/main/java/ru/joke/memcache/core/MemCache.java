package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.StoreConfiguration;
import ru.joke.memcache.core.events.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

final class MemCache<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final CacheConfiguration configuration;
    private final EntryMetadataFactory entryMetadataFactory;
    private final Set<EntryMetadata<?, K>> entriesMetadata;
    private final List<CacheEntryEventListener<K, V>> listeners;
    private final ThreadLocal<Entry<K, V>> oldEntryContainer = new ThreadLocal<>();
    private final boolean eternal;
    private volatile Map<K, Entry<K, V>>[] segments;

    public MemCache(@Nonnull CacheConfiguration configuration) {
        this.configuration = configuration;
        this.eternal = configuration.expirationConfiguration().eternal();
        this.segments = createSegments();
        this.listeners = new CopyOnWriteArrayList<>(configuration.registeredEventListeners());
        this.entryMetadataFactory = new EntryMetadataFactory(configuration);
        final long maxElements = configuration.storeConfiguration().maxElements();
        this.entriesMetadata = new ConcurrentSkipListSet<>() {
            @Override
            public boolean add(EntryMetadata<?, K> metadata) {
                final boolean overflow = size() >= maxElements;

                if (overflow) {
                    if (contains(metadata)) {
                        return false;
                    }

                    // Removing by remove(..) may be run concurrent - its normal
                    synchronized (this) {

                        // Firstly we will try to remove expired entries if possible
                        if (!eternal && size() >= maxElements) {
                            clearExpired();
                        }

                        while (size() >= maxElements) {

                            final EntryMetadata<?, K> removedEntry = first();
                            if (removedEntry != null) {
                                MemCache.this.remove(removedEntry.key());
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
        final Map<K, Entry<K, V>> segment = computeSegment(key);
        final Entry<K, V> entry = segment.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        entry.metadata.onUsage();

        return Optional.of(entry.value);
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
        final Map<K, Entry<K, V>>[] newSegments = createSegments();
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

        final Map<K, Entry<K, V>> segment = computeSegment(key);
        final Entry<K, V> newEntry = segment.computeIfAbsent(
                key,
                k -> {
                    final V value = valueFunction.apply(k);
                    if (value == null) {
                        return null;
                    }

                    final Entry<K, V> result = new Entry<>(
                            value,
                            this.entryMetadataFactory.create(k)
                    );
                    this.oldEntryContainer.set(result);

                    this.entriesMetadata.add(result.metadata);
                    return result;
                }
        );

        if (newEntry == null || this.oldEntryContainer.get() == null) {
            return Optional.empty();
        }

        this.oldEntryContainer.remove();

        final Optional<V> newValue = Optional.of(newEntry.value);
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

        final Map<K, Entry<K, V>> segment = computeSegment(key);
        final Entry<K, V> newEntry = segment.compute(
                key,
                (k, v) -> {
                    this.oldEntryContainer.set(v);

                    if (v == null && oldValue != null || v != null && oldValue != v.value) {
                        return v;
                    }

                    if (newValue == null && v == null) {
                        return null;
                    } else if (newValue == null) {
                        this.entriesMetadata.remove(v.metadata);
                        return null;
                    }

                    final Entry<K, V> result = new Entry<>(
                            newValue,
                            v == null ? this.entryMetadataFactory.create(k) : v.metadata
                    );

                    if (v == null) {
                        this.entriesMetadata.add(result.metadata);
                    }

                    return result;
                }
        );

        final Entry<K, V> oldEntry = this.oldEntryContainer.get();
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

    boolean eternal() {
        return this.eternal;
    }

    void clearExpired() {
        this.entriesMetadata.forEach(metadata -> {
            final long idleExpirationTime = System.currentTimeMillis() - this.configuration.expirationConfiguration().idleExpirationTimeout();
            if (metadata.expiredByTtlAt() <= System.currentTimeMillis() || metadata.lastAccessed() <= idleExpirationTime) {
                // eviction of element from cache data
                compute(metadata.key(), (k, v) -> {
                    this.entriesMetadata.remove(metadata);
                    return null;
                }, true, true);
            }
        });
    }

    private Optional<V> computeIfPresent(
            @Nonnull K key,
            @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction,
            boolean returnOldValue) {
        final Map<K, Entry<K, V>> segment = computeSegment(key);
        final Entry<K, V> newEntry = segment.computeIfPresent(
                key,
                (k, v) -> {
                    final V newVal = remappingFunction.apply(k, v.value);
                    this.oldEntryContainer.set(v);

                    if (newVal == null) {
                        this.entriesMetadata.remove(v.metadata);
                        return null;
                    }

                    return new Entry<>(newVal, v.metadata);
                }
        );

        final Entry<K, V> oldEntry = this.oldEntryContainer.get();
        if (oldEntry == null) {
            return Optional.empty();
        }

        final Optional<V> oldValue = Optional.of(oldEntry.value);
        final Optional<V> newValue = newEntry == null ? Optional.empty() : Optional.of(newEntry.value);
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

        final Map<K, Entry<K, V>> segment = computeSegment(key);
        final Entry<K, V> newEntry = segment.compute(
                key,
                (k, v) -> {
                    this.oldEntryContainer.set(v);

                    final V newVal = remappingFunction.apply(k, v == null ? null : v.value);
                    if (newVal == null && v == null) {
                        return null;
                    } else if (newVal == null) {
                        this.entriesMetadata.remove(v.metadata);
                        return null;
                    } else if (v != null && v.value.equals(newVal)) {
                        return v;
                    }

                    final Entry<K, V> result = new Entry<>(
                            newVal,
                            v == null ? this.entryMetadataFactory.create(k) : v.metadata
                    );

                    if (v == null) {
                        this.entriesMetadata.add(result.metadata);
                    }

                    return result;
                }
        );

        final Entry<K, V> oldEntry = this.oldEntryContainer.get();
        if (newEntry == null && oldEntry == null) {
            return Optional.empty();
        } else if (newEntry == oldEntry) {
            this.oldEntryContainer.remove();
            return Optional.empty();
        }

        final Optional<V> oldValue = oldEntry == null ? Optional.empty() : Optional.of(oldEntry.value);
        final Optional<V> newValue = newEntry == null ? Optional.empty() : Optional.of(newEntry.value);
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

    private Map<K, Entry<K, V>> computeSegment(final K key) {
        final int keyHash = key.hashCode();
        final int hash = keyHash ^ (keyHash >>> 16);
        return this.segments[this.segments.length - 1 & hash];
    }

    private Map<K, Entry<K, V>>[] createSegments() {
        final StoreConfiguration storeConfiguration = configuration.storeConfiguration();
        final int concurrencyLevel = storeConfiguration.concurrencyLevel();
        final int segmentsCount = concurrencyLevel % 2 == 1 ? concurrencyLevel + 1 : concurrencyLevel;

        @SuppressWarnings("unchecked")
        final Map<K, Entry<K, V>>[] segments = new ConcurrentHashMap[segmentsCount];
        for (int i = 0; i < segmentsCount; i++) {
            segments[i] = new ConcurrentHashMap<>((int) storeConfiguration.maxElements() / segmentsCount);
        }

        return segments;
    }

    private record Entry<K, V>(V value, EntryMetadata<?, K> metadata) {
    }
}
