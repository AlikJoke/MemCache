package ru.joke.memcache.core.heap;

import ru.joke.memcache.core.Cache;
import ru.joke.memcache.core.CacheConfiguration;
import ru.joke.memcache.core.CacheManager;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultCacheManager implements CacheManager, Closeable {

    private final Map<String, MemCache<?, ?>> caches = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaningThreadPool;

    private DefaultCacheManager() {
        this.cleaningThreadPool = Executors.newSingleThreadScheduledExecutor();
        this.cleaningThreadPool.scheduleAtFixedRate(
                () -> caches.values().forEach(MemCache::clearExpired),
                1,
                1,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void createCache(@Nonnull CacheConfiguration configuration) {
        final MemCache<?, ?> cache = new MemCache<>(configuration);
        this.caches.put(cache.getName(), cache);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName) {
        @SuppressWarnings("unchecked")
        final Cache<K, V> cache = (Cache<K, V>) this.caches.get(cacheName);
        return Optional.ofNullable(cache);
    }

    @Nonnull
    @Override
    public Set<String> getCacheNames() {
        return this.caches.keySet();
    }

    @Override
    public void close() {
        this.cleaningThreadPool.shutdown();
    }
}
