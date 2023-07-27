package ru.joke.memcache.core;

import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import java.util.List;

public interface CacheConfiguration {

    StoreConfiguration storeConfiguration();

    EvictionConfiguration evictionConfiguration();

    List<CacheEntryEventListener<?, ?>> registeredListeners();

    void registerListener(@Nonnull CacheEntryEventListener<?, ?> listener);

    void unregisterListener(@Nonnull CacheEntryEventListener<?, ?> listener);
}
