package ru.joke.memcache.clustering.listeners;

import ru.joke.cache.bus.core.Cache;
import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.cache.bus.core.CacheEventListenerRegistrar;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of the {@linkplain CacheEventListenerRegistrar} for MemCache.
 *
 * @see CacheEventListenerRegistrar
 * @author Alik
 */
@ThreadSafe
@Immutable
public final class MemCacheCacheEventListenerRegistrar implements CacheEventListenerRegistrar {

    private final String listenerId;

    public MemCacheCacheEventListenerRegistrar() {
        this(UUID.randomUUID().toString());
    }

    public MemCacheCacheEventListenerRegistrar(@Nonnull String listenerId) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
    }

    @Override
    public <K extends Serializable, V extends Serializable> void registerFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new MemCache2BusEntryEventListener<>(this.listenerId, cacheBus, cache.getName());
        cache.registerEventListener(listener);
    }

    @Override
    public <K extends Serializable, V extends Serializable> void unregisterFor(
            @Nonnull CacheBus cacheBus,
            @Nonnull Cache<K, V> cache) {

        final CacheEventListener<K, V> listener = new MemCache2BusEntryEventListener<>(this.listenerId, cacheBus, cache.getName());
        cache.unregisterEventListener(listener);
    }
}
