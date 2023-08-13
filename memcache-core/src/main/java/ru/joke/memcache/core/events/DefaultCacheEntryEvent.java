package ru.joke.memcache.core.events;

import ru.joke.memcache.core.MemCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * The implementation of a cache element change event.
 *
 * @param key       the key of the changed element, cannot be {@code null}.
 * @param oldValue  the old value of the changed element, cannot be {@code null}.
 * @param newValue  the new value of the changed element, cannot be {@code null}.
 * @param eventType the type of the event, cannot be {@code null}.
 * @param source    the source cache, cannot be {@code null}.
 * @param <K>       the type of the cache keys
 * @param <V>       the type of the cache values
 * @author Alik
 * @see CacheEntryEvent
 */
public record DefaultCacheEntryEvent<K extends Serializable, V extends Serializable>(
        @Nonnull K key,
        @Nonnull Optional<V> oldValue,
        @Nonnull Optional<V> newValue,
        @Nonnull EventType eventType,
        @Nonnull MemCache<K, V> source
) implements CacheEntryEvent<K, V> {

    public DefaultCacheEntryEvent {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(source, "source");
    }

    public DefaultCacheEntryEvent(
            @Nonnull K key,
            @Nullable V oldValue,
            @Nullable V newValue,
            @Nonnull EventType eventType,
            @Nonnull MemCache<K, V> source) {
        this(key, Optional.ofNullable(oldValue), Optional.ofNullable(newValue), eventType, source);
    }
}
