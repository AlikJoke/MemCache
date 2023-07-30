package ru.joke.memcache.core.events;

import ru.joke.memcache.core.Cache;

import javax.annotation.Nonnull;
import java.io.Serializable;

public interface CacheEntriesEvent<K extends Serializable, V extends Serializable> {

    @Nonnull
    EventType eventType();

    @Nonnull
    Cache<K, V> source();
}
