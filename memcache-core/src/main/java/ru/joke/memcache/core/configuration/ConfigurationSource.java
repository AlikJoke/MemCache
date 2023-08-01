package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashSet;
import java.util.Set;

public interface ConfigurationSource {

    /**
     * Returns the cache configuration from the source.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    Configuration pull();

    /**
     * Returns the default cache configuration source, manually configurable.
     *
     * @return cannot be {@code null}.
     * @see SimpleConfigurationSource
     */
    @Nonnull
    static SimpleConfigurationSource createDefault() {
        return new SimpleConfigurationSource();
    }

    /**
     * Manually configurable cache configuration source.
     *
     * @author Alik
     * @implSpec The implementation is not thread-safe, so the reference to the source can only be passed
     * to the memcache configuration object. When publishing the reference externally, the result is undefined
     * if the source is modified in other threads.
     * @see ConfigurationSource
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

        @Nonnull
        public SimpleConfigurationSource setCleaningPoolSize(@Nonnegative int cleaningPoolSize) {
            this.cleaningPoolSize = cleaningPoolSize;
            return this;
        }

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
