package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An abstract representation of a cache, independent of the specific store type implementation.<br>
 * Supports basic operations for working with the cache:
 *
 * <ul>
 * <li>Retrieving an object from the cache by key</li>
 * <li>Removing an object from the cache by key</li>
 * <li>Adding an item to the cache</li>
 * <li>Clearing the cache contents</li>
 * <ul>
 *
 * @param <K> the type of the cache keys, must be serializable
 * @param <V> the type of the cache values, must be serializable
 * @author Alik
 */
public interface MemCache<K extends Serializable, V extends Serializable> extends Lifecycle {

    /**
     * Returns the name of the cache.
     *
     * @return cannot be {@code null}.
     */
    @Nonnull
    String name();

    /**
     * Retrieves the value from the cache based on the key, if it exists in the cache.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value the cache associated with the specified key, wrapped in {@link Optional};
     * the value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> get(@Nonnull K key);

    /**
     * Removes an element from the cache based on the key. Returns the value previously associated with the key.
     * If an element with the given key does not exist, returns {@code Optional.empty()}.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value previously associated with the key, wrapped in {@link Optional}. The value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> remove(@Nonnull K key);

    /**
     * Removes an element from the cache based on the key if value equal to current bounded with key value.
     *
     * @param key   the key of the element in the cache, cannot be {@code null}.
     * @param value the value to removing, cannot be {@code null}.
     * @return {@code true} if the cache contain such key and associated value and element was removed, {@code false} otherwise.
     */
    boolean remove(@Nonnull K key, @Nonnull V value);

    /**
     * Adds an element to the cache. Replaces the existing element with a new value if the element already exists the cache.
     *
     * @param key   the key of the element, cannot {@code null}.
     * @param value the value of the element, can be {@code null}.
     * @return the value previously associated with the key, wrapped in {@link Optional}. The value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> put(@Nonnull K key, @Nullable V value);

    /**
     * Adds an element the cache if there is no value associated with the given key.
     *
     * @param key   the key of the element, cannot be {@code null}.
     * @param value the value of the element, can be {@code null}.
     * @return {@code true} if the value was added to cache,
     * {@code false} otherwise if another value already bounded to this key.
     */
    Optional<V> putIfAbsent(@Nonnull K key, @Nullable V value);

    /**
     * Clears this cache.
     */
    void clear();

    /**
     * Merges an existing element in the cache with a new value according to the provided merge function.
     * This operation allows avoiding conflicts when multiple threads modify the same element
     * (e.g., when modifying an object that contains a collection or an associative array) concurrently.<br>
     * If the element does not exist in the cache, it will be added to the cache without merging. <br>
     * The merge function should return {@code null} if the value for the key should be removed from the cache. <br>
     * The implementation of this operation should be thread-safe and take into account the possibility of
     * concurrent modification of the element in the cache by another thread.
     *
     * @param key           the key of the element in the cache; cannot be {@code null}.
     * @param value         the new value of the element to add or modify; cannot be {@code null}.
     * @param mergeFunction the merge function to merge the new value with the existing value in the cache; cannot be {@code null}.
     * @return the new value associated with the specified key, or {@code Optional.empty()} if no value is associated with the key
     */
    Optional<V> merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    /**
     * Computes the value of an element in the cache if it does not already exist.
     * If an element with the given key already exists, it will be returned and no computation will occur.
     * The semantics of this operation are similar to the {@linkplain ConcurrentMap#computeIfAbsent(Object, Function)} method.
     *
     * @param key           the key of the element in the cache; cannot {@code null}.
     * @param valueFunction the function to compute the value of the element if it is absent in the cache; cannot be {@code null}.
     * @return the value of the element from the cache (either existing the time of the call or added as a result), wrapped in {@link Optional}.
     */
    @CheckReturnValue
    @Nonnull
    Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction);

    @CheckReturnValue
    @Nonnull
    Optional<V> compute(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    @CheckReturnValue
    @Nonnull
    Optional<V> computeIfPresent(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    boolean replace(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> getAsync(@Nonnull K key);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> removeAsync(@Nonnull K key);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Boolean> removeAsync(@Nonnull K key, @Nonnull V value);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V> >putAsync(@Nonnull K key, @Nullable V value);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> putIfAbsentAsync(@Nonnull K key, @Nullable V value);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Void> clearAsync();

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> mergeAsync(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeIfAbsentAsync(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeIfPresentAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Boolean> replaceAsync(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue);

    /**
     * Returns cache configuration.
     *
     * @return the cache configuration, cannot be {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    CacheConfiguration configuration();

    boolean registerEventListener(@Nonnull CacheEntryEventListener<K, V> listener);

    boolean deregisterEventListener(@Nonnull CacheEntryEventListener<K, V> listener);
}
