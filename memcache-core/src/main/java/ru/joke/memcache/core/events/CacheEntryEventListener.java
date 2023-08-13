package ru.joke.memcache.core.events;

import ru.joke.memcache.core.configuration.CacheConfiguration;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * Cache element event listener.<br>
 * The listener must be registered in the cache either directly ({@linkplain ru.joke.memcache.core.MemCache#registerEventListener(CacheEntryEventListener)})
 * or through the cache configuration ({@linkplain CacheConfiguration.Builder#addCacheEntryEventListener(CacheEntryEventListener)}}.
 *
 * @param <K> the type of the cache keys
 * @param <V> the type of the cache values
 * @author Alik
 * @see CacheEntryEvent
 * @see CacheEntriesEvent
 */
public interface CacheEntryEventListener<K extends Serializable, V extends Serializable> {

    /**
     * Handler called upon cache element change.
     *
     * @param event cache element change event, cannot be {@code null}.
     * @see CacheEntryEvent
     */
    void onEvent(@Nonnull CacheEntryEvent<? extends K, ? extends V> event);

    /**
     * Handler called upon cache element batch change.
     *
     * @param event cache element batch change event, cannot be {@code null}.
     * @see CacheEntriesEvent
     */
    void onBatchEvent(@Nonnull CacheEntriesEvent<? extends K, ? extends V> event);
}
