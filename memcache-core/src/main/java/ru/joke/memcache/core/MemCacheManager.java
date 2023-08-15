package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of a cache manager responsible for managing caches and their lifecycle.
 *
 * @author Alik
 * @see MemCache
 * @see Lifecycle
 */
public interface MemCacheManager extends Lifecycle {

    /**
     * Creates a cache based on its configuration, initializes it, and adds it to the list of
     * caches managed by the cache manager.
     *
     * @param configuration the configuration of the cache to create, cannot be {@code null}.
     * @return {@code true} if the cache was successfully created and initialized, {@code false} otherwise.
     * @see CacheConfiguration
     * @see MemCache
     */
    boolean createCache(@Nonnull CacheConfiguration configuration);

    /**
     * Removes a cache from the list of caches managed by the cache manager based on its unique name.
     *
     * @param cacheName unique name of the cache, cannot be {@code null}.
     * @return {@code true} if the cache was successfully removed from the managed list, {@code false} otherwise.
     */
    boolean removeCache(@Nonnull String cacheName);

    /**
     * Returns the cache with the specified name if a cache managed by the cache manager with that name exists.
     *
     * @param cacheName unique name of the cache, cannot be {@code null}.
     * @param <K>       the type of the cache keys
     * @param <V>       the type of the cache values
     * @return the cache with the specified name, or {@linkplain Optional#empty()} if the cache with such name does not exist.
     */
    @Nonnull
    @CheckReturnValue
    <K extends Serializable, V extends Serializable> Optional<MemCache<K, V>> getCache(@Nonnull String cacheName);

    /**
     * Returns the cache with the specified name if a cache managed by the cache manager with that name exists.
     *
     * @param cacheName unique name of the cache, cannot be {@code null}.
     * @param keysType type of cache keys, cannot be {@code null}.
     * @param valuesType type of cache values, cannot be {@code null}.
     * @param <K>       the type of the cache keys
     * @param <V>       the type of the cache values
     * @return the cache with the specified name, or {@linkplain Optional#empty()} if the cache with such name does not exist.
     */
    @Nonnull
    @CheckReturnValue
    <K extends Serializable, V extends Serializable> Optional<MemCache<K, V>> getCache(@Nonnull String cacheName, @Nonnull Class<K> keysType, @Nonnull Class<V> valuesType);

    /**
     * Returns a list of names (identifiers) of caches managed by this cache manager.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    Set<String> getCacheNames();
}
