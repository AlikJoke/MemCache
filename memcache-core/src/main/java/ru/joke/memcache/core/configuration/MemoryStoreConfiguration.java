package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * The configuration of storing cache elements in memory.<br>
 * For manual building, use the builder {@code ru.joke.memcache.core.configuration.MemoryStoreConfiguration#builder()}.
 *
 * @author Alik
 * @see CacheConfiguration
 */
public interface MemoryStoreConfiguration {

    /**
     * Returns the probable concurrency level (count of concurrent threads that can read/write cache elements).
     *
     * @return the probable concurrency level, should be positive.
     */
    int concurrencyLevel();

    /**
     * Returns the size of the data container by number of entries. Eviction occurs after the container size exceeds the maximum count.
     *
     * @return the size of the data container by number of entries, should be positive.
     */
    int maxEntries();

    /**
     * Returns a cache memory store configuration builder based on the Java API.
     *
     * @return builder, cannot be {@code null}.
     * @see Builder
     */
    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Cache memory store configuration builder.
     *
     * @author Alik
     */
    @NotThreadSafe
    class Builder {

        private int maxEntries;
        private int concurrencyLevel;

        /**
         * Sets the size of the data container by number of entries.
         *
         * @param maxEntries the size of the data container by number of entries, should be positive.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setMaxEntries(final int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        /**
         * Sets the probable concurrency level (count of concurrent threads that can read/write cache elements).
         *
         * @param concurrencyLevel the probable concurrency level, should be positive.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setConcurrencyLevel(final int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

        /**
         * Performs the creation of the configuration of memory storing cache elements
         * based on the data passed to the builder.
         *
         * @return cannot be {@code null}.
         * @see MemoryStoreConfiguration
         */
        @Nonnull
        public MemoryStoreConfiguration build() {
            if (concurrencyLevel < 1) {
                throw new InvalidConfigurationException("Concurrency level must be at least 1");
            } else if (maxEntries < 1) {
                throw new InvalidConfigurationException("Max entries count must be at least 1");
            }

            final int concurrencyLevel = Math.min(this.concurrencyLevel, this.maxEntries);
            return new MemoryStoreConfiguration() {
                @Override
                public int concurrencyLevel() {
                    return concurrencyLevel;
                }

                @Override
                public int maxEntries() {
                    return maxEntries;
                }

                @Override
                public String toString() {
                    return "MemoryStoreConfiguration{" +
                            "maxEntries=" + maxEntries() +
                            ", concurrencyLevel=" + concurrencyLevel() +
                            '}';
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }
                    if (!(o instanceof final MemoryStoreConfiguration that)) {
                        return false;
                    }

                    return that.maxEntries() == maxEntries
                            && that.concurrencyLevel() == concurrencyLevel;
                }

                @Override
                public int hashCode() {
                    int result = 31;
                    result = 31 * result + maxEntries;
                    result = 31 * result + concurrencyLevel;
                    return result;
                }
            };
        }
    }
}