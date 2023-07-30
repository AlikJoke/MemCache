package ru.joke.memcache.clustering.adapters;

import ru.joke.cache.bus.core.Cache;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@ThreadSafe
public final class MemCacheBusAdapter<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final ru.joke.memcache.core.Cache<K, V> memCache;

    public MemCacheBusAdapter(@Nonnull ru.joke.memcache.core.Cache<K, V> memCache) {
        this.memCache = memCache;
    }

    @Override
    public String getName() {
        return this.memCache.getName();
    }

    @Nonnull
    @Override
    public Optional<V> get(@Nonnull K key) {
        return this.memCache.get(key);
    }

    @Override
    public void evict(@Nonnull K key) {
        this.memCache.remove(key);
    }

    @Nonnull
    @Override
    public Optional<V> remove(@Nonnull K key) {
        return this.memCache.remove(key);
    }

    @Override
    public void put(@Nonnull K key, @Nullable V value) {
        this.memCache.put(key, value);
    }

    @Override
    public void putIfAbsent(@Nonnull K key, @Nullable V value) {
        this.memCache.putIfAbsent(key, value);
    }

    @Override
    public void clear() {
        this.memCache.clear();
    }

    @Override
    public void merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunc) {
        this.memCache.merge(key, value, mergeFunc);
    }

    @Nonnull
    @Override
    public Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunc) {
        return this.memCache.computeIfAbsent(key, valueFunc);
    }

    @Override
    public void registerEventListener(@Nonnull CacheEventListener<K, V> listener) {
        if (listener instanceof CacheEntryEventListener<?, ?>) {
            @SuppressWarnings("unchecked")
            final CacheEntryEventListener<K, V> eventListener = (CacheEntryEventListener<K, V>) listener;
            this.memCache.registerEventListener(eventListener);
        } else {
            throw new ClassCastException("Cache listener implementation must implement " + CacheEntryEventListener.class.getCanonicalName());
        }
    }

    @Override
    public void unregisterEventListener(@Nonnull CacheEventListener<K, V> listener) {

        if (listener instanceof CacheEntryEventListener<?, ?>) {
            @SuppressWarnings("unchecked")
            final CacheEntryEventListener<K, V> eventListener = (CacheEntryEventListener<K, V>) listener;
            this.memCache.deregisterEventListener(eventListener);
        } else {
            throw new ClassCastException("Cache listener implementation must implement " + CacheEntryEventListener.class.getCanonicalName());
        }
    }
}
