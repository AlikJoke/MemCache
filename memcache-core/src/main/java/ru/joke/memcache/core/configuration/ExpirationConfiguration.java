package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * The configuration of expiration time for cache elements.<br>
 * For manual building, use the builder {@code ru.joke.memcache.core.configuration.ExpirationConfiguration#builder()}.
 *
 * @author Alik
 * @see CacheConfiguration
 */
public interface ExpirationConfiguration {

    /**
     * Returns the maximum amount of time, in milliseconds, that cache entries can remain idle.
     * If no operations are performed on entries within the maximum idle time, the entries expire.<br>
     * A value of {@code -1} disables expiration.
     *
     * @return the idle expiration timeout in milliseconds.
     */
    long idleTimeout();

    /**
     * Returns the maximum amount of time, in milliseconds, that cache entries can exist.
     * After reaching their lifespan, cache entries expire.<br>
     * A value of {@code -1} disables expiration.
     *
     * @return the maximum amount of time, in milliseconds, that cache entries can exist.
     */
    long lifespan();

    /**
     * Returns the sign of the eternal storage of cache elements (without expiration in time).
     *
     * @return the sign of the eternal storage of cache elements.
     */
    default boolean eternal() {
        return idleTimeout() == -1 && lifespan() == -1;
    }

    /**
     * Returns a cache expiration configuration builder.
     *
     * @return builder, cannot be {@code null}.
     * @see ExpirationConfiguration.Builder
     */
    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Cache expiration configuration builder.
     *
     * @author Alik
     */
    @NotThreadSafe
    class Builder {

        private long lifespan;
        private long idleTimeout;
        private boolean eternal;

        /**
         * Sets the maximum amount of time, in milliseconds, that cache entries can exist.
         * After reaching their lifespan, cache entries expire.<br>
         * A value of {@code -1} disables expiration.
         *
         * @param lifespan the maximum amount of time, in milliseconds, that cache entries can exist; can be {@code -1} or positive value.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setLifespan(final long lifespan) {
            this.lifespan = lifespan;
            return this;
        }

        /**
         * Sets the maximum amount of time, in milliseconds, that cache entries can remain idle.
         * If no operations are performed on entries within the maximum idle time, the entries expire.<br>
         * A value of {@code -1} disables expiration.
         *
         * @param idleTimeout the idle expiration timeout in milliseconds, can be {@code -1} or positive value.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setIdleTimeout(final long idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        /**
         * Sets the sign of the eternal storage of cache elements (without expiration in time).
         *
         * @param eternal the sign of the eternal storage of cache elements.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setEternal(final boolean eternal) {
            this.eternal = eternal;
            return this;
        }

        /**
         * Performs the creation of the configuration of expiration time for cache elements
         * based on the data passed to the builder.
         *
         * @return cannot be {@code null}.
         * @see ExpirationConfiguration
         */
        @Nonnull
        public ExpirationConfiguration build() {
            if (idleTimeout < -1 || lifespan < -1) {
                throw new InvalidConfigurationException("Expiration timeout must be non negative");
            }

            final long idleTimeout = this.eternal ? -1 : this.idleTimeout < 0 ? this.lifespan : this.idleTimeout;
            final long lifespan = this.eternal ? -1 : this.lifespan < 0 ? this.idleTimeout : this.lifespan;

            return new ExpirationConfiguration() {
                @Override
                public long idleTimeout() {
                    return idleTimeout;
                }

                @Override
                public long lifespan() {
                    return lifespan;
                }

                @Override
                public String toString() {
                    return "ExpirationConfiguration{" +
                            "lifespan=" + lifespan() +
                            ", idleTimeout=" + idleTimeout() +
                            '}';
                }

                @Override
                public int hashCode() {
                    int result = 31;
                    result = 31 * result + (int) lifespan;
                    result = 31 * result + (int) idleTimeout;
                    return result;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }
                    if (!(o instanceof ExpirationConfiguration that)) {
                        return false;
                    }

                    return that.lifespan() == lifespan && that.idleTimeout() == idleTimeout;
                }
            };
        }
    }
}
