package ru.joke.memcache.core.configuration;

import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface CacheConfiguration {

    @Nonnull
    String cacheName();

    @Nonnull
    StoreConfiguration storeConfiguration();

    @Nonnull
    EvictionPolicy evictionPolicy();

    @Nonnull
    ExpirationConfiguration expirationConfiguration();

    <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> registeredEventListeners();

    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    class Builder {

        private String cacheName;
        private StoreConfiguration storeConfiguration;
        private EvictionPolicy evictionPolicy;
        private ExpirationConfiguration expirationConfiguration;
        private List<CacheEntryEventListener<?, ?>> listeners = new ArrayList<>();

        @Nonnull
        public Builder setCacheName(@Nonnull final String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        @Nonnull
        public Builder setStoreConfiguration(@Nonnull final StoreConfiguration storeConfiguration) {
            this.storeConfiguration = storeConfiguration;
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
            return new CacheConfiguration() {
                @Override
                @Nonnull
                public String cacheName() {
                    return cacheName;
                }

                @Override
                @Nonnull
                public StoreConfiguration storeConfiguration() {
                    return storeConfiguration;
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
                @SuppressWarnings("unchecked")
                public <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> registeredEventListeners() {
                    return listeners
                            .stream()
                            .map(l -> (CacheEntryEventListener<K, V>) l)
                            .collect(Collectors.toList());
                }
            };
        }
    }
}
