package ru.joke.memcache.core.events;

import ru.joke.memcache.core.Cache;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

public record DefaultCacheEntriesEvent<K extends Serializable, V extends Serializable>(
        @Nonnull EventType eventType,
        @Nonnull Cache<K, V> source
) implements CacheEntriesEvent<K, V> {

    public DefaultCacheEntriesEvent {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(source, "source");
    }
}

