package ru.joke.memcache.core.configuration;

import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface CacheConfiguration {

    @Nonnull
    String cacheName();

    @Nonnull
    MemoryStoreConfiguration memoryStoreConfiguration();

    @Nonnull
    Optional<PersistentStoreConfiguration> persistentStoreConfiguration();

    @Nonnull
    EvictionPolicy evictionPolicy();

    @Nonnull
    ExpirationConfiguration expirationConfiguration();

    @Nonnull
    <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> registeredEventListeners();

    enum EvictionPolicy {

        LFU,

        LRU,

        MRU,

        FIFO,

        LIFO
    }

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    class Builder {

        private String cacheName;
        private MemoryStoreConfiguration memoryStoreConfiguration;
        private PersistentStoreConfiguration persistentStoreConfiguration;
        private EvictionPolicy evictionPolicy;
        private ExpirationConfiguration expirationConfiguration;
        private List<CacheEntryEventListener<?, ?>> listeners = new ArrayList<>();

        @Nonnull
        public Builder setCacheName(@Nonnull final String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        @Nonnull
        public Builder setMemoryStoreConfiguration(@Nonnull final MemoryStoreConfiguration memoryStoreConfiguration) {
            this.memoryStoreConfiguration = memoryStoreConfiguration;
            return this;
        }

        @Nonnull
        public Builder setPersistentStoreConfiguration(@Nullable final PersistentStoreConfiguration persistentStoreConfiguration) {
            this.persistentStoreConfiguration = persistentStoreConfiguration;
            return this;
        }

        @Nonnull
        public Builder setEvictionPolicy(@Nonnull final EvictionPolicy evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        @Nonnull
        public Builder setExpirationConfiguration(@Nonnull final ExpirationConfiguration expirationConfiguration) {
            this.expirationConfiguration = expirationConfiguration;
            return this;
        }

        @Nonnull
        public Builder setCacheEntryEventListeners(@Nonnull final List<CacheEntryEventListener<?, ?>> listeners) {
            this.listeners = new ArrayList<>(listeners);
            return this;
        }

        @Nonnull
        public Builder addCacheEntryEventListener(@Nonnull final CacheEntryEventListener<?, ?> listener) {
            this.listeners.add(listener);
            return this;
        }

        @Nonnull
        public CacheConfiguration build() {
            final var persistentStoreConfig = Optional.ofNullable(this.persistentStoreConfiguration);
            return new CacheConfiguration() {
                @Override
                @Nonnull
                public String cacheName() {
                    return cacheName;
                }

                @Override
                @Nonnull
                public MemoryStoreConfiguration memoryStoreConfiguration() {
                    return memoryStoreConfiguration;
                }

                @Nonnull
                @Override
                public Optional<PersistentStoreConfiguration> persistentStoreConfiguration() {
                    return persistentStoreConfig;
                }

                @Override
                @Nonnull
                public EvictionPolicy evictionPolicy() {
                    return evictionPolicy;
                }

                @Override
                @Nonnull
                public ExpirationConfiguration expirationConfiguration() {
                    return expirationConfiguration;
                }

                @Override
                @Nonnull
                @SuppressWarnings("unchecked")
                public <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> registeredEventListeners() {
                    return listeners
                            .stream()
                            .map(l -> (CacheEntryEventListener<K, V>) l)
                            .collect(Collectors.toList());
                }

                @Override
                public String toString() {
                    return "CacheConfiguration{" +
                            "cacheName=" + cacheName() +
                            ", evictionPolicy=" + evictionPolicy() +
                            ", memoryStoreConfiguration=" + memoryStoreConfiguration() +
                            ", persistentStoreConfiguration=" + persistentStoreConfiguration() +
                            ", expirationConfiguration=" + expirationConfiguration +
                            '}';
                }
            };
        }
    }
}
