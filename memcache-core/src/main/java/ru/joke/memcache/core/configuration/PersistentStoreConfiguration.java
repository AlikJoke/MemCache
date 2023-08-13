package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

/**
 * The configuration of persistent storage for cache elements (used when
 * the application stops, if 'quick' cache warming is required when the
 * application resumes operation).<br>
 * For manual building, use the builder {@code ru.joke.memcache.core.configuration.PersistentStoreConfiguration#builder()}.
 *
 * @author Alik
 * @see CacheConfiguration
 */
public interface PersistentStoreConfiguration {

    /**
     * Returns the persistent store location (directory path).
     *
     * @return the persistent store location, can be {@code null}.
     */
    @Nullable
    String location();

    /**
     * Returns the unique id of the persistent store (maybe unique application id / name).
     *
     * @return the unique id of the persistent store, cannot be {@code null}.
     */
    @Nonnull
    String uid();

    /**
     * Returns a cache persistent storage configuration builder based on the Java API.
     *
     * @return builder, cannot be {@code null}.
     * @see Builder
     */
    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Cache persistent store configuration builder.
     *
     * @author Alik
     */
    @NotThreadSafe
    class Builder {

        private String location;
        private String uid;

        /**
         * Sets the persistent store location (directory path).
         *
         * @param location the persistent store location (directory path), can be {@code null}.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setLocation(@Nullable final String location) {
            this.location = location;
            return this;
        }

        /**
         * Sets the unique id of the persistent store.
         *
         * @param uid the unique id of the persistent store, cannot be {@code null}.
         * @return the builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setUid(@Nonnull final String uid) {
            this.uid = uid;
            return this;
        }

        /**
         * Performs the creation of the configuration of persistent store for cache elements
         * based on the data passed to the builder.
         *
         * @return cannot be {@code null}.
         * @see PersistentStoreConfiguration
         */
        @Nonnull
        public PersistentStoreConfiguration build() {
            if (this.uid == null || this.uid.isBlank()) {
                throw new InvalidConfigurationException("Storage uid must be not blank");
            }

            return new PersistentStoreConfiguration() {
                @Nullable
                @Override
                public String location() {
                    return location;
                }

                @Nonnull
                @Override
                public String uid() {
                    return uid;
                }

                @Override
                public String toString() {
                    return "PersistentStoreConfiguration{" +
                            "location=" + location() +
                            ", uid=" + uid() +
                            '}';
                }

                @Override
                public int hashCode() {
                    int result = uid.hashCode();
                    result = 31 * result + (location == null ? 0 : location.hashCode());
                    return result;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }
                    if (!(o instanceof PersistentStoreConfiguration that)) {
                        return false;
                    }

                    return that.uid().equals(uid) && Objects.equals(that.location(), location);
                }
            };
        }
    }
}