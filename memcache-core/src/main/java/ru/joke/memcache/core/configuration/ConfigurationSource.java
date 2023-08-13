package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.Set;

/**
 * Source of the MemCache caching provider configuration. Depending on the application
 * configuration, it can be configured using either XML or Java API, or an alternative
 * option with its own implementation of this interface.
 *
 * @author Alik
 * @see Configuration
 * @see ConfigurationSource#createDefault()
 * @see XmlConfigurationSource
 */
public interface ConfigurationSource {

    /**
     * Returns the MemCache caching provider configuration from the source.
     *
     * @return cannot be {@code null}.
     * @see Configuration
     */
    @Nonnull
    Configuration pull();

    /**
     * Returns the default caching configuration source, manually configurable.
     *
     * @return cannot be {@code null}.
     * @see SimpleConfigurationSource
     */
    @Nonnull
    static SimpleConfigurationSource createDefault() {
        return new SimpleConfigurationSource();
    }

    /**
     * Manually configurable caching configuration source.
     *
     * @author Alik
     * @implSpec The implementation is not thread-safe, so the reference to the source can only be passed
     * to the memcache configuration object. When publishing the reference externally, the result is undefined
     * if the source is modified in other threads.
     * @see ConfigurationSource
     * @see XmlConfigurationSource
     */
    @NotThreadSafe
    final class SimpleConfigurationSource implements ConfigurationSource {

        private final Set<CacheConfiguration> configurations = new HashSet<>();
        private int cleaningPoolSize = 1;
        private int asyncCacheOpsParallelismLevel = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);

        @Nonnull
        @Override
        public Configuration pull() {
            final Set<CacheConfiguration> cacheConfigurations = Set.copyOf(this.configurations);
            if (asyncCacheOpsParallelismLevel <= 0) {
                throw new InvalidConfigurationException("Async cache operations parallelism level must be positive");
            } else if (cleaningPoolSize <= 0) {
                throw new InvalidConfigurationException("Cleaning pool size must be positive");
            }

            return new Configuration() {
                @Override
                @Nonnull
                public Set<CacheConfiguration> cacheConfigurations() {
                    return cacheConfigurations;
                }

                @Override
                @Nonnegative
                public int cleaningPoolSize() {
                    return cleaningPoolSize;
                }

                @Override
                public int asyncCacheOpsParallelismLevel() {
                    return asyncCacheOpsParallelismLevel;
                }

                @Override
                public String toString() {
                    return "Configuration{" +
                            "cacheConfigurations=" + cacheConfigurations() +
                            ", cleaningPoolSize=" + cleaningPoolSize() +
                            ", asyncCacheOpsParallelismLevel=" + asyncCacheOpsParallelismLevel() +
                            '}';
                }
            };
        }

        /**
         * Adds the configuration of a single cache to the source.
         *
         * @param configuration the cache configuration, cannot be {@code null}.
         * @return the source for further building, cannot be {@code null}.
         * @see CacheConfiguration
         */
        @Nonnull
        public SimpleConfigurationSource add(@Nonnull CacheConfiguration configuration) {
            this.configurations.add(configuration);
            return this;
        }

        /**
         * Adds multiple cache configurations to the source.
         *
         * @param configurations the cache configurations, cannot be {@code null}.
         * @return the source for further building, cannot be {@code null}.
         * @see CacheConfiguration
         */
        @Nonnull
        public SimpleConfigurationSource addAll(@Nonnull Set<CacheConfiguration> configurations) {
            this.configurations.addAll(configurations);
            return this;
        }

        /**
         * Clears the cache source.
         *
         * @return the source for further building, cannot be {@code null}.
         */
        @Nonnull
        public SimpleConfigurationSource clear() {
            this.configurations.clear();
            return this;
        }

        /**
         * Sets the expired elements cleaning pool size.<br>
         * The default value is 1.
         * Depending on the number of caches and their size, a larger value than the default value may be required.
         *
         * @param cleaningPoolSize the expired elements cleaning pool size, should be positive.
         * @return the source for further building, cannot be {@code null}.
         */
        @Nonnull
        public SimpleConfigurationSource setCleaningPoolSize(@Nonnegative int cleaningPoolSize) {
            this.cleaningPoolSize = cleaningPoolSize;
            return this;
        }

        /**
         * Sets the size of the pool of asynchronous operations on cache elements.<br>
         * The default value is half of the available processors.
         *
         * @param asyncCacheOpsParallelismLevel the size of the pool of asynchronous operations on cache elements, should be positive.
         * @return the source for further building, cannot be {@code null}.
         */
        @Nonnull
        public SimpleConfigurationSource setAsyncCacheOpsParallelismLevel(@Nonnegative int asyncCacheOpsParallelismLevel) {
            this.asyncCacheOpsParallelismLevel = asyncCacheOpsParallelismLevel;
            return this;
        }

        @Override
        public String toString() {
            return "SimpleConfigurationSource{" +
                    "configurations=" + configurations +
                    ", cleaningPoolSize=" + cleaningPoolSize +
                    ", asyncCacheOpsParallelismLevel=" + asyncCacheOpsParallelismLevel +
                    '}';
        }
    }
}
