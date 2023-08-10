package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.Lifecycle;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.ConfigurationSource;
import ru.joke.memcache.core.configuration.ExpirationConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;

import java.io.Serializable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class InternalMemCacheManagerTest {

    private CacheConfiguration cacheConfiguration1;
    private CacheConfiguration cacheConfiguration2;

    @BeforeEach
    void setUp() {
        this.cacheConfiguration1 = buildCacheConfig("test1", 20, 2, true, -1, -1);
        this.cacheConfiguration2 = buildCacheConfig("test2", 40, 4, false, 1000, 100);
    }

    @Test
    public void testInitializationWithDefaultConfigurationSource() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager()) {
            doInitializationOfCacheManager(cacheManager);
        }
    }

    @Test
    public void testInitializationWithProvidedConfigurationSource() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager(createSimpleConfigurationSource())) {
            doInitializationOfCacheManager(cacheManager);
            makeConfigurationChecksAfterInitialization(cacheManager);
        }
    }

    @Test
    public void testInitializationWithProvidedConfiguration() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager(createSimpleConfigurationSource().pull())) {
            doInitializationOfCacheManager(cacheManager);
            makeConfigurationChecksAfterInitialization(cacheManager);
        }
    }

    @Test
    public void testCacheCreationInDefaultCacheManager() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager()) {
            cacheManager.initialize();

            final boolean result = cacheManager.createCache(this.cacheConfiguration1);
            assertTrue(result, "Cache must be created");
            assertTrue(cacheManager.getCacheNames().contains(this.cacheConfiguration1.cacheName()), "Cache must present after creation");

            assertFalse(cacheManager.createCache(this.cacheConfiguration1), "Cache should already present in cache manager");
        }
    }

    @Test
    public void testCacheCreationWithProvidedConfiguration() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager(createSimpleConfigurationSource())) {
            cacheManager.initialize();

            boolean result = cacheManager.createCache(this.cacheConfiguration1);
            assertFalse(result, "Cache already must present in cache manager");

            final var cacheConfig3 = buildCacheConfig("test3", 10, 1, true, -1, -1);
            result = cacheManager.createCache(cacheConfig3);

            assertTrue(result, "Cache must be added in cache manager");
            assertTrue(cacheManager.getCacheNames().contains(cacheConfig3.cacheName()), "Cache must present after creation");
        }
    }

    @Test
    public void testCacheRemovalWithProvidedConfiguration() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager(createSimpleConfigurationSource())) {
            cacheManager.initialize();

            boolean result = cacheManager.removeCache(this.cacheConfiguration1.cacheName());
            assertTrue(result, "Cache must be removed from cache manager");

            result = cacheManager.removeCache(this.cacheConfiguration1.cacheName());
            assertFalse(result, "Cache must be already removed from cache manager");

            assertFalse(cacheManager.getCacheNames().contains(this.cacheConfiguration1.cacheName()), "Cache must not present after removal");
        }
    }

    @Test
    public void testCacheGetWhenConfigurationProvided() {
        try (final InternalMemCacheManager cacheManager = new InternalMemCacheManager(createSimpleConfigurationSource())) {
            cacheManager.initialize();

            final Optional<MemCache<Serializable, Serializable>> result = cacheManager.getCache(this.cacheConfiguration1.cacheName());
            assertTrue(result.isPresent(), "Cache must present in cache manager");
            assertEquals(Lifecycle.ComponentStatus.RUNNING, result.orElseThrow().status(), "Cache must be in " + Lifecycle.ComponentStatus.RUNNING + " state");
        }
    }

    @Test
    public void testCacheManagerShutdown() {
        final InternalMemCacheManager cacheManager = new InternalMemCacheManager(createSimpleConfigurationSource());
        final Optional<MemCache<Serializable, Serializable>> cache;
        try (cacheManager) {
            cacheManager.initialize();
            cache = cacheManager.getCache(this.cacheConfiguration1.cacheName());
        }

        assertEquals(Lifecycle.ComponentStatus.TERMINATED, cacheManager.status(), "Cache manager should be stopped");

        assertTrue(cache.isPresent(), "Cache must present in cache manager");
        assertEquals(Lifecycle.ComponentStatus.TERMINATED, cache.orElseThrow().status(), "Cache must be terminated after termination of cache manager");
    }

    private void makeConfigurationChecksAfterInitialization(final InternalMemCacheManager cacheManager) {
        assertTrue(cacheManager.getCacheNames().contains(this.cacheConfiguration1.cacheName()), "CacheManager must contain cache with given name");
        assertTrue(cacheManager.getCacheNames().contains(this.cacheConfiguration2.cacheName()), "CacheManager must contain cache with given name");
    }

    private void doInitializationOfCacheManager(final InternalMemCacheManager cacheManager) {

        assertEquals(Lifecycle.ComponentStatus.UNAVAILABLE, cacheManager.status(), "Status should be " + Lifecycle.ComponentStatus.UNAVAILABLE + " when cache manager is not initialized");

        cacheManager.initialize();

        assertEquals(Lifecycle.ComponentStatus.RUNNING, cacheManager.status(), "Status should be " + Lifecycle.ComponentStatus.RUNNING + " after initialization of cache manager");
    }

    private ConfigurationSource createSimpleConfigurationSource() {
        return ConfigurationSource
                .createDefault()
                    .setCleaningPoolSize(1)
                    .setAsyncCacheOpsParallelismLevel(2)
                    .add(this.cacheConfiguration1)
                    .add(this.cacheConfiguration2);
    }

    private CacheConfiguration buildCacheConfig(
            final String cacheName,
            final int maxEntries,
            final int concurrencyLevel,
            final boolean eternal,
            final long lifespan,
            final long idleTimeout) {
        return CacheConfiguration
                .builder()
                    .setCacheName(cacheName)
                    .setEvictionPolicy(CacheConfiguration.EvictionPolicy.LFU)
                    .setMemoryStoreConfiguration(
                            MemoryStoreConfiguration
                                    .builder()
                                        .setMaxEntries(maxEntries)
                                        .setConcurrencyLevel(concurrencyLevel)
                                    .build()
                    )
                    .setExpirationConfiguration(
                            ExpirationConfiguration
                                    .builder()
                                        .setEternal(eternal)
                                        .setLifespan(lifespan)
                                        .setIdleTimeout(idleTimeout)
                                    .build()
                    )
                .build();
    }
}
