package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Set;

/**
 * MemCache provider configuration.<br>
 * To build the configuration, see {@linkplain ru.joke.memcache.core.configuration.ConfigurationSource}
 * and its implementations: {@linkplain ru.joke.memcache.core.configuration.XmlConfigurationSource}
 * for XML file-based configuration and {@linkplain ru.joke.memcache.core.configuration.ConfigurationSource#createDefault()}
 * for Java API-based configuration.
 *
 * @author Alik
 * @see CacheConfiguration
 * @see ConfigurationSource
 * @see XmlConfigurationSource
 */
public interface Configuration {

    /**
     * Returns a list of cache configurations.
     *
     * @return cannot be {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    Set<CacheConfiguration> cacheConfigurations();

    /**
     * Returns the expired elements cleaning pool size. <br> The default value is 1.
     * Depending on the number of caches and their size, a larger value than the default value may be required.
     *
     * @return the expired elements cleaning pool size, should be positive.
     */
    @Nonnegative
    int cleaningPoolSize();

    /**
     * Returns the size of the pool of asynchronous operations on cache elements.<br>
     * The default value is half of the available processors.
     *
     * @return the size of the pool of asynchronous operations, should be positive.
     */
    @Nonnegative
    int asyncCacheOpsParallelismLevel();
}
