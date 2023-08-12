package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.events.CacheEntryEventListener;
import ru.joke.memcache.core.stats.MemCacheStatistics;

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
 * An abstract representation of a named cache that stores data in Java heap memory.
 * It is the root abstraction of the library.<br>
 * It supports the following core operations (synchronous):
 * <ul>
 * <li>Getting a value by key</li>
 * <li>Adding a value by key</li>
 * <li>Removing a value by key </li>
 * <li>Removing by key and value (if the value matches)</li>
 * <li>Replacing a key's value (if the value matches)</li>
 * <li>Merge and compute operations for a key's value</li>
 * <li>Clearing the cache contents</li>
 * <ul>
 * Each synchronous operation has an asynchronous counterpart based on {@linkplain CompletableFuture}.<br>
 * Cache is local, but can be clustered using the CacheBus library.
 * As a result, there is a limitation on keys and values of cache items: they must be serializable.<br>
 * Cache supports various eviction and expiration policies for items, as well as capabilities
 * for saving cache items to disk when the application stops, for quick cache repopulation upon resuming operation.<br>
 * The cache supports management of event listeners, which can be registered on the cache.
 * Listeners are notified of every action performed on the cache elements. <br>
 * Additionally, the cache maintains statistics during its lifecycle, including the number of cache elements,
 * the number of successful key retrievals from the cache, unsuccessful retrievals, and successful/unsuccessful
 * additions/removals to/from the cache.
 *
 * @param <K> the type of the cache keys, must be serializable
 * @param <V> the type of the cache values, must be serializable
 * @author Alik
 * @see Lifecycle
 * @see MemCacheStatistics
 * @see CacheConfiguration
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
     * Returns cache configuration.
     *
     * @return the cache configuration, cannot be {@code null}.
     * @see CacheConfiguration
     */
    @Nonnull
    CacheConfiguration configuration();

    /**
     * Registers an event listener for the elements in this cache.
     *
     * @param listener the listener to register, cannot be {@code null}.
     * @return {@code true} if the listener was registered, {@code false} otherwise.
     * @see CacheEntryEventListener
     */
    boolean registerEventListener(@Nonnull CacheEntryEventListener<K, V> listener);

    /**
     * Unregisters an event listener for the elements in this cache.
     *
     * @param listener the listener to unregister, cannot be {@code null}.
     * @return {@code true} if the listener was unregistered, {@code false} otherwise.
     * @see CacheEntryEventListener
     * @see CacheEntryEventListener
     */
    boolean deregisterEventListener(@Nonnull CacheEntryEventListener<K, V> listener);

    /**
     * Returns statistics about operations on the elements in this cache.
     *
     * @return cannot be {@code null}.
     * @see MemCacheStatistics
     */
    @Nonnull
    MemCacheStatistics statistics();

    /**
     * Retrieves the value from the cache based on the key, if it exists in the cache.<br>
     * Asynchronous version of this operation: {@linkplain #getAsync(Serializable)}.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value in the cache associated with the specified key, wrapped in {@link Optional};
     * the value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> get(@Nonnull K key);

    /**
     * Removes an element from the cache based on the key. Returns the value previously associated with the key.
     * If an element with the given key does not exist, returns {@code Optional.empty()}.<br>
     * Asynchronous version of this operation: {@linkplain #removeAsync(Serializable)}.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value previously associated with the key, wrapped in {@link Optional}. The value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> remove(@Nonnull K key);

    /**
     * Removes an element from the cache based on the key if value equal to current associated with key value.<br>
     * Asynchronous version of this operation: {@linkplain #removeAsync(Serializable, Serializable)}.
     *
     * @param key   the key of the element in the cache, cannot be {@code null}.
     * @param value the value to removing, cannot be {@code null}.
     * @return {@code true} if the cache contain such key and associated value and element was removed, {@code false} otherwise.
     */
    boolean remove(@Nonnull K key, @Nonnull V value);

    /**
     * Adds an element to the cache. Replaces the existing element with a new value if the element already exists the cache.<br>
     * If a value equal to null is passed, this key is removed from the cache.<br>
     * Asynchronous version of this operation: {@linkplain #putAsync(Serializable, Serializable)}.
     *
     * @param key   the key of the element, cannot be {@code null}.
     * @param value the value of the element, can be {@code null}.
     * @return the value previously associated with the key, wrapped in {@link Optional}. The value may be absent.
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> put(@Nonnull K key, @Nullable V value);

    /**
     * Adds an element to the cache if there is no value associated with the given key.<br>
     * Asynchronous version of this operation: {@linkplain #putIfAbsentAsync(Serializable, Serializable)}.
     *
     * @param key   the key of the element, cannot be {@code null}.
     * @param value the value of the element, can be {@code null}.
     * @return the value previously associated with the key, wrapped in {@link Optional}. The value may be absent.
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
     * concurrent modification of the element in the cache by another thread.<br>
     * Asynchronous version of this operation: {@linkplain #mergeAsync(Serializable, Serializable, BiFunction)}.
     *
     * @param key           the key of the element in the cache; cannot be {@code null}.
     * @param value         the new value of the element to add or modify; cannot be {@code null}.
     * @param mergeFunction the merge function to merge the new value with the existing value in the cache; cannot be {@code null}.
     * @return the new value associated with the specified key, or {@code Optional.empty()} if no value is associated with the key
     */
    @Nonnull
    @CheckReturnValue
    Optional<V> merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    /**
     * Computes the value of an element in the cache if it does not already exist.
     * If an element with the given key already exists, it will be returned and no computation will occur.<br>
     * The semantics of this operation are similar to the {@linkplain ConcurrentMap#computeIfAbsent(Object, Function)} method.<br>
     * The mapping function should not modify this map during computation.<br>
     * Asynchronous version of this operation: {@linkplain #computeIfAbsentAsync(Serializable, Function)}.
     *
     * @param key             the key of the element in the cache; cannot be {@code null}.
     * @param mappingFunction the function to compute the value of the element if it is absent in the cache; cannot be {@code null}.
     * @return the new value associated with the specified key, or {@link Optional#empty()} if none.
     */
    @CheckReturnValue
    @Nonnull
    Optional<V> computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> mappingFunction);

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value (or null if there is no current mapping).<br>
     * The remapping function should return {@code null} if the value for the key should be removed from the cache. <br>
     * The semantics of this operation are similar to the {@linkplain ConcurrentMap#compute(Object, BiFunction)} method.<br>
     * The remapping function should not modify this map during computation.<br>
     * Asynchronous version of this operation: {@linkplain #computeAsync(Serializable, BiFunction)}.
     *
     * @param key               the key of the element in the cache; cannot be {@code null}.
     * @param remappingFunction the function to compute the value of the element; cannot be {@code null}.
     * @return @return the new value associated with the specified key, or {@link Optional#empty()} if none.
     */
    @CheckReturnValue
    @Nonnull
    Optional<V> compute(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * If the value for the specified key is present and non-null, attempts to compute a new mapping given the key and its current mapped value.<br>
     * The remapping function should return {@code null} if the value for the key should be removed from the cache. <br>
     * The semantics of this operation are similar to the {@linkplain ConcurrentMap#computeIfPresent(Object, BiFunction)} method.<br>
     * The remapping function should not modify this map during computation.<br>
     * Asynchronous version of this operation: {@linkplain #computeIfPresentAsync(Serializable, BiFunction)}.
     *
     * @param key               the key of the element in the cache; cannot be {@code null}.
     * @param remappingFunction the function to compute the value of the element if it is not absent in the cache; cannot be {@code null}.
     * @return the new value associated with the specified key, or {@link Optional#empty()} if none.
     */
    @CheckReturnValue
    @Nonnull
    Optional<V> computeIfPresent(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Replaces the value associated with the given key if the value in the cache matches
     * the one passed as an argument to the method. If the new value is {@code null}, the element
     * with this key is removed from the cache (in case the old value matches the passed one).<br>
     * Asynchronous version of this operation: {@linkplain #replaceAsync(Serializable, Serializable, Serializable)}.
     *
     * @param key      the key of the element in the cache; cannot be {@code null}.
     * @param oldValue the old value of the element in the cache; can be {@code null}.
     * @param newValue the new value of the element to replace in the cache; can be {@code null}.
     * @return {@code true} if the value was replaced in the cache, {@code false} otherwise.
     */
    boolean replace(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue);

    /**
     * Asynchronous version of the operation: {@linkplain #get(Serializable)}.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value in the cache associated with the specified key wrapped in {@linkplain CompletableFuture}.
     * @see #get(Serializable)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> getAsync(@Nonnull K key);

    /**
     * Asynchronous version of the operation: {@linkplain #remove(Serializable)}.
     *
     * @param key the key of the element in the cache, cannot be {@code null}.
     * @return the value previously associated with the key wrapped in {@linkplain CompletableFuture}.
     * @see #remove(Serializable)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> removeAsync(@Nonnull K key);

    /**
     * Asynchronous version of the operation: {@linkplain #remove(Serializable, Serializable)}.
     *
     * @param key   the key of the element in the cache, cannot be {@code null}.
     * @param value the value to removing, cannot be {@code null}.
     * @return {@code true} wrapped in {@linkplain CompletableFuture} if the cache contain
     * such key and associated value and element was removed, {@code false} in {@linkplain CompletableFuture} otherwise..
     * @see #remove(Serializable, Serializable)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Boolean> removeAsync(@Nonnull K key, @Nonnull V value);

    /**
     * Asynchronous version of the operation: {@linkplain #put(Serializable, Serializable)}.
     *
     * @param key   the key of the element in the cache, cannot be {@code null}.
     * @param value the value of the element to add, cannot be {@code null}.
     * @return the value previously associated with the key wrapped in {@linkplain CompletableFuture}.
     * @see #put(Serializable, Serializable)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> putAsync(@Nonnull K key, @Nullable V value);

    /**
     * Asynchronous version of the operation: {@linkplain #putIfAbsent(Serializable, Serializable)}.
     *
     * @param key   the key of the element in the cache, cannot be {@code null}.
     * @param value the value of the element to add, cannot be {@code null}.
     * @return the value previously associated with the key wrapped in {@linkplain CompletableFuture}.
     * @see #putIfAbsent(Serializable, Serializable)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> putIfAbsentAsync(@Nonnull K key, @Nullable V value);

    /**
     * Asynchronous version of the operation: {@linkplain #clear()}.
     *
     * @return the result of the clearing wrapped in {@linkplain CompletableFuture}.
     * @see #clear()
     */
    @Nonnull
    CompletableFuture<Void> clearAsync();

    /**
     * Asynchronous version of the operation: {@linkplain #merge(Serializable, Serializable, BiFunction)}.
     *
     * @param key           the key of the element in the cache, cannot be {@code null}.
     * @param value         the new value of the element to add or modify; cannot be {@code null}.
     * @param mergeFunction the merge function to merge the new value with the existing value in the cache; cannot be {@code null}.
     * @return wrapped in {@linkplain CompletableFuture} the new value associated with the specified key,
     * or Optional.empty() if no value is associated with the key.
     * @see #merge(Serializable, Serializable, BiFunction)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> mergeAsync(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    /**
     * Asynchronous version of the operation: {@linkplain #computeIfAbsent(Serializable, Function)}.
     *
     * @param key               the key of the element in the cache, cannot be {@code null}.
     * @param remappingFunction the function to compute the value of the element if it is absent in the cache; cannot be {@code null}.
     * @return the new value associated with the specified key, or Optional.empty() if none, wrapped in {@linkplain CompletableFuture}.
     * @see #computeIfAbsent(Serializable, Function)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeIfAbsentAsync(@Nonnull K key, @Nonnull Function<? super K, ? extends V> remappingFunction);

    /**
     * Asynchronous version of the operation: {@linkplain #compute(Serializable, BiFunction)}.
     *
     * @param key               the key of the element in the cache, cannot be {@code null}.
     * @param remappingFunction the function to compute the value of the element; cannot be {@code null}.
     * @return the new value associated with the specified key, or Optional.empty() if none, wrapped in {@linkplain CompletableFuture}.
     * @see #compute(Serializable, BiFunction)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Asynchronous version of the operation: {@linkplain #computeIfPresent(Serializable, BiFunction)}.
     *
     * @param key               the key of the element in the cache, cannot be {@code null}.
     * @param remappingFunction the function to compute the value of the element if it is not absent in the cache; cannot be {@code null}.
     * @return the new value associated with the specified key, or Optional.empty() if none, wrapped in {@linkplain CompletableFuture}.
     * @see #computeIfPresent(Serializable, BiFunction)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeIfPresentAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    /**
     * Asynchronous version of the operation: {@linkplain #replace(Serializable, Serializable, Serializable)}.
     *
     * @param key      the key of the element in the cache, cannot be {@code null}.
     * @param oldValue the old value of the element in the cache; can be {@code null}.
     * @param newValue the new value of the element to replace in the cache; can be
     * @return {@code true} if the value was replaced in the cache, {@code false} otherwise, wrapped in {@linkplain CompletableFuture}.
     * @see #replace(Serializable, Serializable, Serializable)
     */
    @Nonnull
    @CheckReturnValue
    CompletableFuture<Boolean> replaceAsync(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue);
}
