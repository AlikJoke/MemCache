package ru.joke.memcache.core.events;

import ru.joke.memcache.core.Cache;
import ru.joke.memcache.core.events.CacheEntriesEvent;
import ru.joke.memcache.core.events.EventType;

import javax.annotation.Nonnull;
import java.io.Serializable;

public record DefaultCacheEntriesEvent<K extends Serializable, V extends Serializable>(
        @Nonnull EventType eventType,
        @Nonnull Cache<K, V> source
) implements CacheEntriesEvent<K, V> {
}

