package ru.joke.memcache.core.events;

import ru.joke.memcache.core.MemCache;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

/**
 * The implementation of a batch cache element change event.
 *
 * @param eventType the type of the event, cannot be {@code null}.
 * @param source    the source cache, cannot be {@code null}.
 * @param <K>       the type of the cache keys
 * @param <V>       the type of the cache values
 * @author Alik
 * @see CacheEntriesEvent
 */
public record DefaultCacheEntriesEvent<K extends Serializable, V extends Serializable>(
        @Nonnull EventType eventType,
        @Nonnull MemCache<K, V> source
) implements CacheEntriesEvent<K, V> {

    public DefaultCacheEntriesEvent {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(source, "source");
    }
}

