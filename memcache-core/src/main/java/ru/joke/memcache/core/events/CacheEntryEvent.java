package ru.joke.memcache.core.events;

import ru.joke.memcache.core.MemCache;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;

/**
 * Cache element change event.
 *
 * @param <K> the type of cache keys
 * @param <V> the type of cache values
 * @see CacheEntriesEvent
 * @see CacheEntryEventListener
 */
public interface CacheEntryEvent<K extends Serializable, V extends Serializable> {

    /**
     * Returns the cache element key for which the event is generated.
     *
     * @return the cache element key, cannot be {@code null}.
     */
    @Nonnull
    K key();

    /**
     * Returns the new value of the cache element after the change.
     *
     * @return cannot be {@code null}, but can be {@code Optional.empty()}.
     */
    @Nonnull
    Optional<V> newValue();

    /**
     * Returns the old value of the cache element after the change.
     *
     * @return cannot be {@code null}, but can be {@code Optional.empty()}.
     */
    @Nonnull
    Optional<V> oldValue();

    /**
     * Returns the type of cache element change event.
     *
     * @return the type of cache element change event, cannot be {@code null}.
     * @see EventType
     */
    @Nonnull
    EventType eventType();

    /**
     * Returns the cache source in which the change occurred.
     *
     * @return the cache source in which the change occurred, cannot be {@code null}.
     * @see MemCache
     */
    @Nonnull
    MemCache<K, V> source();
}
