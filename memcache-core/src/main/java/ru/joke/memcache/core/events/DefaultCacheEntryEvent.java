package ru.joke.memcache.core.events;

import ru.joke.memcache.core.Cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

public record DefaultCacheEntryEvent<K extends Serializable, V extends Serializable>(
        @Nonnull K key,
        @Nonnull Optional<V> oldValue,
        @Nonnull Optional<V> newValue,
        @Nonnull EventType eventType,
        @Nonnull Cache<K, V> source
) implements CacheEntryEvent<K, V> {

    public DefaultCacheEntryEvent  {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(source, "source");
    }

    public DefaultCacheEntryEvent(
            @Nonnull K key,
            @Nullable V oldValue,
            @Nullable V newValue,
            @Nonnull EventType eventType,
            @Nonnull Cache<K, V> source) {
        this(key, Optional.ofNullable(newValue), Optional.ofNullable(oldValue), eventType, source);
    }
}
