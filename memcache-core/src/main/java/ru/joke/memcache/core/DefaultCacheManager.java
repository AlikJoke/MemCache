package ru.joke.memcache.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.Configuration;
import ru.joke.memcache.core.configuration.ConfigurationSource;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

@ThreadSafe
public final class DefaultCacheManager implements CacheManager, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCacheManager.class);

    private final Map<String, MemCache<?, ?>> caches = new ConcurrentHashMap<>();
    private ScheduledExecutorService cleaningThreadPool;
    private AsyncOpsInvoker asyncOpsInvoker;
    private List<Future<?>> scheduledCleaningTasks;
    private int cleaningPoolSize;

    @Override
    public synchronized void initialize() {
        this.asyncOpsInvoker = new AsyncOpsInvoker(Runtime.getRuntime().availableProcessors() / 2);
        this.cleaningThreadPool = Executors.newSingleThreadScheduledExecutor(new CleaningThreadFactory());
        this.cleaningPoolSize = 1;
        this.scheduledCleaningTasks = scheduleCleaningTasks(false);
    }

    @Override
    public synchronized void initialize(@Nonnull ConfigurationSource configurationSource) {
        final Configuration configuration = configurationSource.pull();

        this.asyncOpsInvoker = new AsyncOpsInvoker(configuration.asyncCacheOpsParallelismLevel());
        this.cleaningPoolSize = configuration.cleaningPoolSize();
        this.cleaningThreadPool = Executors.newScheduledThreadPool(this.cleaningPoolSize, new CleaningThreadFactory());

        configuration.cacheConfigurations().forEach(cacheConfiguration -> createCache(cacheConfiguration, false));

        this.scheduledCleaningTasks = scheduleCleaningTasks(false);
    }

    @Override
    public synchronized boolean createCache(@Nonnull CacheConfiguration configuration) {
        return createCache(configuration, true);
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
        shutdown();
    }

    @Override
    public synchronized void shutdown() {
        this.cleaningThreadPool.shutdown();
        this.asyncOpsInvoker.close();

        this.caches.values().forEach(MemCache::shutdown);
    }

    private boolean createCache(final CacheConfiguration configuration, final boolean rescheduleCleaningTasks) {

        final MemCache<?, ?> cache = new MemCache<>(configuration, this.asyncOpsInvoker);
        final boolean newCacheAdded = this.caches.putIfAbsent(cache.getName(), cache) == null;
        if (newCacheAdded) {
            cache.initialize();

            if (rescheduleCleaningTasks) {
                this.scheduledCleaningTasks.forEach(f -> f.cancel(true));
                this.scheduledCleaningTasks = scheduleCleaningTasks(true);
            }
        }

        return newCacheAdded;
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
                                .mapToLong(expirationConfig -> Math.min(expirationConfig.idleExpirationTimeout(), expirationConfig.lifespan()))
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

    static class CleaningThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            final Thread result = Thread
                                    .ofVirtual()
                                    .name("MemCache-Cleaning-Thread-", 0)
                                    .uncaughtExceptionHandler((t, e) -> logger.error("Unexpected error from cleaning thread: " + t, e))
                                    .unstarted(r);
            result.setDaemon(true);
            return result;
        }
    }
}
