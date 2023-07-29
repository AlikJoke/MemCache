package ru.joke.memcache.core.heap;

import ru.joke.memcache.core.*;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.Configuration;
import ru.joke.memcache.core.configuration.ConfigurationSource;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

public class DefaultCacheManager implements CacheManager, Closeable {

    private final Map<String, MemCache<?, ?>> caches = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleaningThreadPool;
    private List<Future<?>> scheduledCleaningTasks;
    private int cleaningPoolSize;

    @Override
    public synchronized void init() {
        this.cleaningThreadPool = Executors.newSingleThreadScheduledExecutor();
        this.cleaningPoolSize = 1;
        this.scheduledCleaningTasks = scheduleCleaningTasks(false);
    }

    @Override
    public synchronized void init(@Nonnull ConfigurationSource configurationSource) {
        final Configuration configuration = configurationSource.pull();
        configuration.cacheConfigurations().forEach(this::createCache);

        this.cleaningPoolSize = configuration.cleaningPoolSize();
        this.cleaningThreadPool = Executors.newScheduledThreadPool(this.cleaningPoolSize);
        this.scheduledCleaningTasks = scheduleCleaningTasks(false);
    }

    @Override
    public synchronized void createCache(@Nonnull CacheConfiguration configuration) {
        final MemCache<?, ?> cache = new MemCache<>(configuration);
        this.caches.put(cache.getName(), cache);

        this.scheduledCleaningTasks.forEach(f -> f.cancel(true));
        this.scheduledCleaningTasks = scheduleCleaningTasks(true);
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
    public synchronized void close() {
        this.scheduledCleaningTasks.forEach(f -> f.cancel(true));
        this.cleaningThreadPool.shutdown();
    }

    private List<Future<?>> scheduleCleaningTasks(boolean startImmediately) {

        final List<Future<?>> cleaningTasks = new ArrayList<>(this.cleaningPoolSize);

        final List<MemCache<?, ?>> cleanableCaches =
                this.caches
                        .values()
                        .stream()
                        .filter(MemCache::eternal)
                        .toList();

        final int partsCount = cleanableCaches.size() / this.cleaningPoolSize;
        final List<MemCache<?, ?>> cachesPart = new ArrayList<>();
        for (int i = 1; i <= cleanableCaches.size(); i++) {
            final MemCache<?, ?> cache = cleanableCaches.get(i - 1);
            if (i % partsCount == 0 || i == cleanableCaches.size()) {

                final List<MemCache<?, ?>> cachesToProcessing = new ArrayList<>(cachesPart);
                cachesPart.clear();

                final long minExpirationTimeout =
                        cachesToProcessing
                                .stream()
                                .map(MemCache::getConfiguration)
                                .map(CacheConfiguration::expirationConfiguration)
                                .mapToLong(expirationConfig -> Math.min(expirationConfig.idleExpirationTimeout(), expirationConfig.expirationTimeout()))
                                .min()
                                .orElse(-1);

                if (minExpirationTimeout == -1) {
                    continue;
                }

                final ScheduledFuture<?> taskFuture =
                        this.cleaningThreadPool.scheduleAtFixedRate(
                                () -> cachesToProcessing.forEach(MemCache::clearExpired),
                                startImmediately ? 0 : minExpirationTimeout,
                                minExpirationTimeout,
                                TimeUnit.MILLISECONDS
                        );
                this.scheduledCleaningTasks.add(taskFuture);
            }

            cachesPart.add(cache);
        }

        return cleaningTasks;
    }
}
