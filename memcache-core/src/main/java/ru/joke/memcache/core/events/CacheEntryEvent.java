package ru.joke.memcache.core.events;

import ru.joke.memcache.core.Cache;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;

public interface CacheEntryEvent<K extends Serializable, V extends Serializable> {

    @Nonnull
    K key();

    @Nonnull
    Optional<V> newValue();

    @Nonnull
    Optional<V> oldValue();

    @Nonnull
    EventType eventType();

    @Nonnull
    Cache<K, V> source();
}
