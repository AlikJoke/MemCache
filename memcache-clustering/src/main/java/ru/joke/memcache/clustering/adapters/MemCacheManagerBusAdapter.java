package ru.joke.memcache.clustering.adapters;

import ru.joke.cache.bus.core.Cache;
import ru.joke.cache.bus.core.CacheManager;
import ru.joke.cache.bus.core.state.ComponentState;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class MemCacheManagerBusAdapter implements CacheManager {

    private static final String CACHE_MANAGER_ID = "memcache-manager";

    private final ru.joke.memcache.core.CacheManager memCacheManager;
    private final Map<String, Optional<Cache<Serializable, Serializable>>> cachesMap;

    public MemCacheManagerBusAdapter(@Nonnull ru.joke.memcache.core.CacheManager memCacheManager) {
        this.memCacheManager = Objects.requireNonNull(memCacheManager, "memCacheManager");
        this.cachesMap = new ConcurrentHashMap<>();
    }

    @Nonnull
    @Override
    public <T> T getUnderlyingCacheManager(@Nonnull Class<T> managerType) {
        return Objects.requireNonNull(managerType, "managerType").cast(this.memCacheManager);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        return this.cachesMap.computeIfAbsent(cacheName, this::composeCacheAdapter)
                                .map(this::cast);
    }

    @Nonnull
    @Override
    public ComponentState state() {
        // TODO
        throw new UnsupportedOperationException();
    }

    private <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> composeCacheAdapter(@Nonnull String cacheName) {
        final Optional<ru.joke.memcache.core.Cache<K, V>> memCache = this.memCacheManager.getCache(cacheName);
        return memCache.map(MemCacheBusAdapter::new);
    }

    private <K extends Serializable, V extends Serializable> Cache<K, V> cast(Cache<Serializable, Serializable> cache) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> result = (Cache<K, V>) cache;
        return result;
    }
}
