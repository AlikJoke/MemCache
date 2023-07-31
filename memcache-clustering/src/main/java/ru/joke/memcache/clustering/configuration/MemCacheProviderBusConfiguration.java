package ru.joke.memcache.clustering.configuration;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import ru.joke.memcache.clustering.adapters.MemCacheManagerBusAdapter;
import ru.joke.memcache.clustering.listeners.MemCacheCacheEventListenerRegistrar;
import ru.joke.memcache.core.MemCacheManager;

import javax.annotation.Nonnull;

/**
 * Implementation of the MemCache caching provider configuration that simplifies event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see CacheBus
 */
public final class MemCacheProviderBusConfiguration extends CacheProviderConfigurationTemplate {

    public MemCacheProviderBusConfiguration(@Nonnull MemCacheManager cacheManager) {
        super(
                new MemCacheManagerBusAdapter(cacheManager),
                new MemCacheCacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull MemCacheManager cacheManager) {
        return new MemCacheProviderBusConfiguration(cacheManager);
    }
}