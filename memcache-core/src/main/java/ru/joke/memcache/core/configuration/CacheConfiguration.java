package ru.joke.memcache.core.configuration;

import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Configuration (descriptor) of the MemCache cache.<br>
 * Specifies various storage settings (in memory and in persistent storage when the application stops),
 * element eviction policy or expiration time, as well as the list of listeners for this
 * cache.<br>
 * The configuration can be built using either XML-based configuration or the Java API.
 * For XML-based configuration, refer to the static factory methods from
 * {@linkplain ru.joke.memcache.core.configuration.XmlConfigurationSource}.
 * For Java API configuration, refer to the {@linkplain ru.joke.memcache.core.configuration.CacheConfiguration#builder()}.
 *
 * @author Alik
 * @see ConfigurationSource
 */
public interface CacheConfiguration {

    /**
     * Returns the unique name of the cache.
     *
     * @return the unique name of the cache, cannot be {@code null}.
     */
    @Nonnull
    String cacheName();

    /**
     * Returns the configuration of storing cache elements in memory.
     *
     * @return cannot be {@code null}.
     * @see MemoryStoreConfiguration
     */
    @Nonnull
    MemoryStoreConfiguration memoryStoreConfiguration();

    /**
     * Returns the configuration of persistent storage for cache elements (used when
     * the application stops, if 'quick' cache warming is required when the
     * application resumes operation).
     *
     * @return cannot be {@code null}, but can be empty {@code Optional.empty()}.
     * @see PersistentStoreConfiguration
     */
    @Nonnull
    Optional<PersistentStoreConfiguration> persistentStoreConfiguration();

    /**
     * Returns the cache element eviction policy when the maximum size of cache
     * elements is exceeded in the storage configuration.
     *
     * @return eviction policy, cannot be {@code null}.
     * @see EvictionPolicy
     */
    @Nonnull
    EvictionPolicy evictionPolicy();

    /**
     * Returns the configuration of expiration time for cache elements.
     *
     * @return cannot be {@code null}.
     * @see ExpirationConfiguration
     */
    @Nonnull
    ExpirationConfiguration expirationConfiguration();

    /**
     * Returns the list of listeners for cache element events.
     *
     * @param <K> the type of the cache keys
     * @param <V> the type of the cache values
     * @return cannot be {@code null}.
     * @see CacheEntryEventListener
     */
    @Nonnull
    <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> eventListeners();

    /**
     * Eviction policy for cache elements when the maximum number of cache elements is exceeded.
     *
     * @author Alik
     */
    enum EvictionPolicy {

        /**
         * Least Frequently Used
         */
        LFU,

        /**
         * Least Recently Used
         */
        LRU,

        /**
         * Most Recently Used
         */
        MRU,

        /**
         * First In First Out
         */
        FIFO,

        /**
         * Last In First Out
         */
        LIFO
    }

    /**
     * Returns a cache configuration builder based on the Java API.
     *
     * @return builder, cannot be {@code null}.
     * @see Builder
     */
    @Nonnull
    static Builder builder() {
        return new Builder();
    }

    /**
     * Cache configuration builder.
     *
     * @author Alik
     * @see CacheConfiguration#builder()
     */
    @NotThreadSafe
    class Builder {

        private String cacheName;
        private MemoryStoreConfiguration memoryStoreConfiguration;
        private PersistentStoreConfiguration persistentStoreConfiguration;
        private EvictionPolicy evictionPolicy;
        private ExpirationConfiguration expirationConfiguration;
        private List<CacheEntryEventListener<?, ?>> listeners = new ArrayList<>();

        /**
         * Sets the name of the cache.
         *
         * @param cacheName the name of the cache, cannot be {@code null}.
         * @return builder, cannot be {@code null}.
         */
        @Nonnull
        public Builder setCacheName(@Nonnull final String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        /**
         * Sets the used configuration for storing elements in memory.
         *
         * @param memoryStoreConfiguration configuration for storing elements in memory, cannot be {@code null}.
         * @return builder, cannot be {@code null}.
         * @see MemoryStoreConfiguration
         */
        @Nonnull
        public Builder setMemoryStoreConfiguration(@Nonnull final MemoryStoreConfiguration memoryStoreConfiguration) {
            this.memoryStoreConfiguration = memoryStoreConfiguration;
            return this;
        }

        /**
         * Sets the used configuration for persistent storage of elements (optional).
         *
         * @param persistentStoreConfiguration configuration for persistent storage of elements, can be {@code null}.
         * @return builder, cannot be {@code null}.
         * @see PersistentStoreConfiguration
         */
        @Nonnull
        public Builder setPersistentStoreConfiguration(@Nullable final PersistentStoreConfiguration persistentStoreConfiguration) {
            this.persistentStoreConfiguration = persistentStoreConfiguration;
            return this;
        }

        /**
         * Sets the element eviction policy.
         *
         * @param evictionPolicy eviction policy, cannot be {@code null}.
         * @return builder, cannot be {@code null}.
         * @see EvictionPolicy
         */
        @Nonnull
        public Builder setEvictionPolicy(@Nonnull final EvictionPolicy evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        /**
         * Sets the configuration for expiration time of cache elements.
         *
         * @param expirationConfiguration the configuration for expiration time of cache elements, cannot be {@code null}.
         * @return builder, cannot be {@code null}.
         * @see ExpirationConfiguration
         */
        @Nonnull
        public Builder setExpirationConfiguration(@Nonnull final ExpirationConfiguration expirationConfiguration) {
            this.expirationConfiguration = expirationConfiguration;
            return this;
        }

        /**
         * Sets the list of used event listeners for the cache.
         *
         * @param listeners the list of used event listeners for the cache, cannot be {@code null}.
         * @return builder, cannot be {@code null}.
         * @see CacheEntryEventListener
         */
        @Nonnull
        public Builder setCacheEntryEventListeners(@Nonnull final List<CacheEntryEventListener<?, ?>> listeners) {
            this.listeners = new ArrayList<>(listeners);
            return this;
        }

        /**
         * Adds the given listener to the list of used event listeners for the cache.
         *
         * @param listener event listener for the cache, cannot be {@code null}.
         * @return builder, cannot be {@code null}.
         * @see CacheEntryEventListener
         */
        @Nonnull
        public Builder addCacheEntryEventListener(@Nonnull final CacheEntryEventListener<?, ?> listener) {
            this.listeners.add(listener);
            return this;
        }

        /**
         * Performs the creation of a cache configuration object based on the data passed to the builder.
         *
         * @return cannot be {@code null}.
         * @see CacheConfiguration
         */
        @Nonnull
        public CacheConfiguration build() {
            if (this.evictionPolicy == null) {
                throw new InvalidConfigurationException("Eviction policy must be not null");
            } else if (this.cacheName == null || this.cacheName.isBlank()) {
                throw new InvalidConfigurationException("Cache name must be not blank string");
            } else if (this.memoryStoreConfiguration == null) {
                throw new InvalidConfigurationException("Memory store configuration must be not null");
            } else if (this.expirationConfiguration == null) {
                throw new InvalidConfigurationException("Expiration configuration must be not null");
            }

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
                public <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> eventListeners() {
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

                @Override
                public int hashCode() {
                    return Objects.hashCode(cacheName);
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }
                    if (!(o instanceof final CacheConfiguration that)) {
                        return false;
                    }

                    return that.cacheName().equals(cacheName);
                }
            };
        }
    }
}
