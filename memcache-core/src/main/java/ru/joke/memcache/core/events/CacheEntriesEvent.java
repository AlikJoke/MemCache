package ru.joke.memcache.core.events;

import ru.joke.memcache.core.MemCache;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Cache element batch change event.
 *
 * @param <K> the type of cache keys
 * @param <V> the type of cache values
 * @see CacheEntryEvent
 * @see CacheEntryEventListener
 */
public interface CacheEntriesEvent<K extends Serializable, V extends Serializable> {

    /**
     * Returns the type of batch change event.
     *
     * @return the type of batch change event, cannot be {@code null}.
     * @see EventType
     */
    @Nonnull
    EventType eventType();

    /**
     * Returns the cache source in which the batch change occurred.
     *
     * @return the cache source in which the batch change occurred, cannot be {@code null}.
     * @see MemCache
     */
    @Nonnull
    MemCache<K, V> source();
}
