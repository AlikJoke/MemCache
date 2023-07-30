package ru.joke.memcache.clustering.configuration;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.configuration.CacheProviderConfiguration;
import ru.joke.cache.bus.core.impl.configuration.CacheProviderConfigurationTemplate;
import ru.joke.memcache.clustering.adapters.MemCacheManagerBusAdapter;
import ru.joke.memcache.clustering.listeners.MemCacheCacheEventListenerRegistrar;
import ru.joke.memcache.core.CacheManager;

import javax.annotation.Nonnull;

/**
 * Implementation of the MemCache caching provider configuration that simplifies event bus setup.
 *
 * @author Alik
 * @see CacheProviderConfiguration
 * @see CacheBus
 */
public final class MemCacheProviderBusConfiguration extends CacheProviderConfigurationTemplate {

    public MemCacheProviderBusConfiguration(@Nonnull CacheManager cacheManager) {
        super(
                new MemCacheManagerBusAdapter(cacheManager),
                new MemCacheCacheEventListenerRegistrar()
        );
    }

    @Nonnull
    public static CacheProviderConfiguration create(@Nonnull CacheManager cacheManager) {
        return new MemCacheProviderBusConfiguration(cacheManager);
    }
}