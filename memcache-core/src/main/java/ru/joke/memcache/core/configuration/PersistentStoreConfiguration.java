package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

public interface PersistentStoreConfiguration {

    String location();

    @Nonnull
    String uid();

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    @NotThreadSafe
    class Builder {

        private String location;
        private String uid;

        @Nonnull
        public Builder setLocation(@Nullable final String location) {
            this.location = location;
            return this;
        }

        @Nonnull
        public Builder setUid(@Nonnull final String uid) {
            this.uid = uid;
            return this;
        }

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
            };
        }
    }
}