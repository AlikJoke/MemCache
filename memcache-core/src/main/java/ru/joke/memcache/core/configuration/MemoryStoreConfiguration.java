package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

public interface MemoryStoreConfiguration {

    int concurrencyLevel();

    int maxEntries();

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    class Builder {

        private int maxEntries;
        private int concurrencyLevel;

        @Nonnull
        public Builder setMaxEntries(final int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        @Nonnull
        public Builder setConcurrencyLevel(final int concurrencyLevel) {
            this.concurrencyLevel = concurrencyLevel;
            return this;
        }

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