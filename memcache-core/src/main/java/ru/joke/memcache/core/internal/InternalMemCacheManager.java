package ru.joke.memcache.core.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.joke.memcache.core.LifecycleException;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.MemCacheException;
import ru.joke.memcache.core.MemCacheManager;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.Configuration;
import ru.joke.memcache.core.configuration.ConfigurationSource;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

@ThreadSafe
public final class InternalMemCacheManager implements MemCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(InternalMemCacheManager.class);

    private final Map<String, MapMemCache<?, ?>> caches = new ConcurrentHashMap<>();
    private final ConfigurationSource configurationSource;
    private final Configuration configuration;
    private volatile ComponentStatus status = ComponentStatus.UNAVAILABLE;
    private ScheduledExecutorService cleaningThreadPool;
    private AsyncOpsInvoker asyncOpsInvoker;
    private List<Future<?>> scheduledCleaningTasks;
    private int cleaningPoolSize;

    public InternalMemCacheManager() {
        this.configurationSource = ConfigurationSource.createDefault();
        this.configuration = null;
    }

    public InternalMemCacheManager(@Nonnull ConfigurationSource configurationSource) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource");
        this.configuration = null;
    }

    public InternalMemCacheManager(@Nonnull Configuration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.configurationSource = null;
    }

    @Override
    public synchronized void initialize() {
        if (this.status != ComponentStatus.UNAVAILABLE) {
            throw new LifecycleException("Status must be " + ComponentStatus.UNAVAILABLE + ". Current state is " + this.status);
        }

        logger.info("Initialization of cache manager was called");

        this.status = ComponentStatus.INITIALIZING;

        try {
            final Configuration configuration =
                    this.configurationSource == null
                        ? this.configuration
                        : this.configurationSource.pull();

            logger.debug("Configuration for initialization {}", configuration);

            this.asyncOpsInvoker = new AsyncOpsInvoker(configuration.asyncCacheOpsParallelismLevel());
            this.cleaningPoolSize = configuration.cleaningPoolSize();
            this.cleaningThreadPool = Executors.newScheduledThreadPool(this.cleaningPoolSize, new CleaningThreadFactory());

            logger.debug("Initialization of caches will be called");
            configuration.cacheConfigurations().forEach(cacheConfiguration -> createCache(cacheConfiguration, false));

            logger.debug("Initialization of caches was completed");

            this.scheduledCleaningTasks = scheduleCleaningTasks(false);
            logger.debug("Cleaning threads was scheduled");
        } catch (RuntimeException ex) {
            this.status = ComponentStatus.FAILED;
            logger.error("Unable to initialize cache manager", ex);
            throw (ex instanceof MemCacheException ? ex : new MemCacheException(ex));
        }

        this.status = ComponentStatus.RUNNING;
        logger.info("Initialization of cache managed was completed");
    }

    @Override
    public synchronized boolean createCache(@Nonnull CacheConfiguration configuration) {
        if (this.status != ComponentStatus.RUNNING && this.status != ComponentStatus.INITIALIZING) {
            throw new LifecycleException("Cache creation available only in " + ComponentStatus.RUNNING + " or " + ComponentStatus.INITIALIZING + " state. Current state is " + this.status);
        }
        
        return createCache(configuration, true);
    }

    @Nonnull
    @Override
    public <K extends Serializable, V extends Serializable> Optional<MemCache<K, V>> getCache(@Nonnull String cacheName) {
        if (this.status != ComponentStatus.RUNNING) {
            throw new LifecycleException("Cache retrieval available only in " + ComponentStatus.RUNNING + " state. Current state is " + this.status);
        }

        @SuppressWarnings("unchecked")
        final MemCache<K, V> cache = (MemCache<K, V>) this.caches.get(cacheName);
        return Optional.ofNullable(cache);
    }

    @Nonnull
    @Override
    public Set<String> getCacheNames() {
        if (this.status != ComponentStatus.RUNNING) {
            throw new LifecycleException("Cache names retrieval available only in " + ComponentStatus.RUNNING + " state. Current state is " + this.status);
        }

        return new HashSet<>(this.caches.keySet());
    }

    @Nonnull
    @Override
    public ComponentStatus status() {
        return this.status;
    }

    @Override
    public synchronized void shutdown() {

        if (this.status != ComponentStatus.RUNNING) {
            throw new LifecycleException("Shutdown available only in " + ComponentStatus.RUNNING + " state. Current state is " + this.status);
        }

        logger.info("Shutdown of cache manager was called");

        this.status = ComponentStatus.STOPPING;

        try {
            this.cleaningThreadPool.shutdown();
            logger.debug("Cleaning thread pool has been shutdown");
            this.asyncOpsInvoker.close();
            logger.debug("Async cache ops pool has been shutdown");

            this.caches.values().forEach(MapMemCache::shutdown);
            logger.debug("Caches has been shutdown");
        } catch (RuntimeException ex) {
            this.status = ComponentStatus.FAILED;
            logger.error("Unable to initialize cache manager", ex);
            throw (ex instanceof MemCacheException ? ex : new MemCacheException(ex));
        }

        this.status = ComponentStatus.TERMINATED;

        logger.info("Shutdown of cache manager was completed");
    }

    private boolean createCache(final CacheConfiguration configuration, final boolean rescheduleCleaningTasks) {

        final EntryMetadataFactory metadataFactory = new EntryMetadataFactory(configuration);
        final PersistentCacheRepository persistentCacheRepository =
                configuration.persistentStoreConfiguration()
                                .map(psc -> new DiskPersistentCacheRepository(configuration, metadataFactory))
                                .map(PersistentCacheRepository.class::cast)
                                .orElseGet(PersistentCacheRepository.NoPersistentCacheRepository::new);

        final MapMemCache<?, ?> cache = new MapMemCache<>(
                configuration,
                this.asyncOpsInvoker,
                persistentCacheRepository,
                metadataFactory
        );
        final boolean newCacheAdded = this.caches.putIfAbsent(cache.name(), cache) == null;
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

        final List<MapMemCache<?, ?>> cleanableCaches =
                this.caches
                        .values()
                        .stream()
                        .filter(MapMemCache::eternal)
                        .toList();

        final int partsCount = cleanableCaches.size() / this.cleaningPoolSize;
        final List<MapMemCache<?, ?>> cachesPart = new ArrayList<>();
        for (int i = 1; i <= cleanableCaches.size(); i++) {
            final MapMemCache<?, ?> cache = cleanableCaches.get(i - 1);
            if (i % partsCount == 0 || i == cleanableCaches.size()) {

                final List<MapMemCache<?, ?>> cachesToProcessing = new ArrayList<>(cachesPart);
                cachesPart.clear();

                final long minExpirationTimeout =
                        cachesToProcessing
                                .stream()
                                .map(MemCache::configuration)
                                .map(CacheConfiguration::expirationConfiguration)
                                .mapToLong(expirationConfig -> Math.min(expirationConfig.idleTimeout(), expirationConfig.lifespan()))
                                .min()
                                .orElse(-1);

                if (minExpirationTimeout == -1) {
                    continue;
                }

                final ScheduledFuture<?> taskFuture =
                        this.cleaningThreadPool.scheduleAtFixedRate(
                                () -> cachesToProcessing.forEach(MapMemCache::clearExpired),
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
