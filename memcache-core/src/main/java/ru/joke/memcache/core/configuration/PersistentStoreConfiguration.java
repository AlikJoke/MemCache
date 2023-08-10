package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.Objects;

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