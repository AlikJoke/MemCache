package ru.joke.memcache.core.heap;

import ru.joke.memcache.core.Cache;
import ru.joke.memcache.core.CacheConfiguration;
import ru.joke.memcache.core.StoreConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class MemCache<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final String name;
    private final CacheConfiguration configuration;
    private final Map<K, Entry<K, V>>[] segments;
    private final EntryMetadataFactory entryMetadataFactory;
    private final Set<EntryMetadata<?, K>> entriesMetadata;

    public MemCache(
            @Nonnull String name,
            @Nonnull CacheConfiguration configuration) {
        this.name = name;
        this.configuration = configuration;
        this.segments = createSegments();
        this.entryMetadataFactory = new EntryMetadataFactory(configuration.evictionConfiguration());
        this.entriesMetadata = new ConcurrentSkipListSet<>() {
            @Override
            public boolean add(EntryMetadata<?, K> metadata) {
                final boolean overflow = size() >= configuration.storeConfiguration().maxElementsInHeapMemory();

                if (overflow) {
                    // Removing by remove(..) may be run concurrent - its normal
                    synchronized (this) {
                        while (size() >= configuration.storeConfiguration().maxElementsInHeapMemory()) {
                            pollFirst();
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
        return this.name;
    }

    @Nonnull
    @Override
    public CacheConfiguration getConfiguration() {
        return this.configuration;
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        final Entry<K, V> entry = computeSegment(key).get(key);
        if (entry == null) {
            return Optional.empty();
        }

        synchronized (entry) {
            entry.metadata().onUsage();
        }

        return Optional.of(entry.value());
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        final Map<K, Entry<K, V>> segment = computeSegment(key);
        final Entry<K, V> entry = segment.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        synchronized (entry) {
            if (segment.remove(key, entry)) {
                this.entriesMetadata.remove(entry.metadata());
                return Optional.of(entry.value());
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean remove(@Nonnull K key, @Nonnull V value) {
        return computeSegment(key).remove(key, value);
    }

    @Nonnull
    @Override
    public Optional<V> put(@Nonnull K key, @Nullable V value) {
        return Optional.ofNullable(computeSegment(key).put(key, value));
    }

    @Override
    public boolean putIfAbsent(@Nonnull K key, @Nullable V value) {
        return computeSegment(key).putIfAbsent(key, value) == null;
    }

    @Override
    public void clear() {
        final Map<K, Entry<K, V>>[] newSegments = createSegments();
        System.arraycopy(newSegments, 0, this.segments, 0, newSegments.length);
    }

    @Override
    public Optional<V> merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction) {
        return Optional.ofNullable(computeSegment(key).merge(key, value, mergeFunction));
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction) {
        return Optional.ofNullable(computeSegment(key).computeIfAbsent(key, valueFunction));
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
            segments[i] = new ConcurrentHashMap<>((int) storeConfiguration.maxElementsInHeapMemory() / segmentsCount);
        }

        return segments;
    }

    record Entry<K, V>(@Nonnull V value, @Nonnull EntryMetadata<?, K> metadata) {
    }
}
