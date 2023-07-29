package ru.joke.memcache.core;

import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.List;

public interface CacheConfiguration {

    String cacheName();

    StoreConfiguration storeConfiguration();

    EvictionConfiguration evictionConfiguration();

    <K extends Serializable, V extends Serializable> List<CacheEntryEventListener<K, V>> registeredListeners();

    void registerListener(@Nonnull CacheEntryEventListener<?, ?> listener);

    void unregisterListener(@Nonnull CacheEntryEventListener<?, ?> listener);
}
