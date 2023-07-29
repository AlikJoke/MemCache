package ru.joke.memcache.core.heap;

import ru.joke.memcache.core.Cache;
import ru.joke.memcache.core.events.CacheEntryEvent;
import ru.joke.memcache.core.events.EventType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;

public record DefaultCacheEntryEvent<K extends Serializable, V extends Serializable>(
        @Nonnull K key,
        @Nonnull Optional<V> oldValue,
        @Nonnull Optional<V> newValue,
        @Nonnull EventType eventType,
        @Nonnull Cache<K, V> source
) implements CacheEntryEvent<K, V> {

    public DefaultCacheEntryEvent(
            @Nonnull K key,
            @Nullable V oldValue,
            @Nullable V newValue,
            @Nonnull EventType eventType,
            @Nonnull Cache<K, V> source) {
        this(key, Optional.ofNullable(newValue), Optional.ofNullable(oldValue), eventType, source);
    }
}
