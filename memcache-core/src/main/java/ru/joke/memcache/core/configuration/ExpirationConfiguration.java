package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

public interface ExpirationConfiguration {

    long idleTimeout();

    long lifespan();

    default boolean eternal() {
        return idleTimeout() == -1 && lifespan() == -1;
    }

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    class Builder {

        private long lifespan;
        private long idleTimeout;
        private boolean eternal;

        @Nonnull
        public Builder setLifespan(final long lifespan) {
            this.lifespan = lifespan;
            return this;
        }

        @Nonnull
        public Builder setIdleTimeout(final long idleTimeout) {
            this.idleTimeout = idleTimeout;
            return this;
        }

        @Nonnull
        public Builder setEternal(final boolean eternal) {
            this.eternal = eternal;
            return this;
        }

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
