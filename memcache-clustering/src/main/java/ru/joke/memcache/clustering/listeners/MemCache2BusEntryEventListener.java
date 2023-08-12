package ru.joke.memcache.clustering.listeners;

import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEntryEventType;
import ru.joke.cache.bus.core.impl.ImmutableCacheEntryEvent;
import ru.joke.memcache.core.events.CacheEntriesEvent;
import ru.joke.memcache.core.events.CacheEntryEvent;
import ru.joke.memcache.core.events.CacheEntryEventListener;
import ru.joke.memcache.core.events.EventType;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.Objects;

/**
 * Implementation of the {@linkplain ru.joke.cache.bus.core.CacheEventListener} for MemCache.
 *
 * @author Alik
 * @see ru.joke.cache.bus.core.CacheEventListener
 * @see CacheEntryEventListener
 */
@ThreadSafe
@Immutable
final class MemCache2BusEntryEventListener<K extends Serializable, V extends Serializable> implements CacheEntryEventListener<K, V>, ru.joke.cache.bus.core.CacheEventListener<K, V> {

    private final String listenerId;
    private final CacheBus cacheBus;
    private final String cacheName;

    public MemCache2BusEntryEventListener(
            @Nonnull String listenerId,
            @Nonnull CacheBus cacheBus,
            @Nonnull String cacheName) {
        this.listenerId = Objects.requireNonNull(listenerId, "listenerId");
        this.cacheBus = Objects.requireNonNull(cacheBus, "cacheBus");
        this.cacheName = Objects.requireNonNull(cacheName, "cacheName");
    }

    @Override
    public void onEvent(@Nonnull CacheEntryEvent<? extends K, ? extends V> cacheEvent) {

        final ru.joke.cache.bus.core.CacheEntryEvent<K, V> busEvent = new ImmutableCacheEntryEvent<>(
                cacheEvent.key(),
                cacheEvent.oldValue().orElse(null),
                cacheEvent.newValue().orElse(null),
                System.currentTimeMillis(),
                convertMemCacheEventType2BusType(cacheEvent.eventType()),
                this.cacheName
        );
        this.cacheBus.send(busEvent);
    }

    @Override
    public void onBatchEvent(@Nonnull CacheEntriesEvent<? extends K, ? extends V> cacheEntriesEvent) {
        final ru.joke.cache.bus.core.CacheEntryEvent<String, V> busEvent = new ImmutableCacheEntryEvent<>(
                ru.joke.cache.bus.core.CacheEntryEvent.ALL_ENTRIES_KEY,
                null,
                null,
                System.currentTimeMillis(),
                convertMemCacheEventType2BusType(cacheEntriesEvent.eventType()),
                this.cacheName
        );
        this.cacheBus.send(busEvent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MemCache2BusEntryEventListener<?, ?> that = (MemCache2BusEntryEventListener<?, ?>) o;

        return listenerId.equals(that.listenerId)
                && cacheName.equals(that.cacheName)
                && cacheBus.equals(that.cacheBus);
    }

    @Override
    public int hashCode() {
        int result = listenerId.hashCode();
        result = 31 * result + cacheBus.hashCode();
        result = 31 * result + cacheName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MemCache2BusEntryEventListener{" +
                "listenerId='" + listenerId + '\'' +
                ", cacheName='" + cacheName +
                '}';
    }

    private CacheEntryEventType convertMemCacheEventType2BusType(final EventType eventType) {
        return switch (eventType) {
            case REMOVED -> CacheEntryEventType.EVICTED;
            case EXPIRED -> CacheEntryEventType.EXPIRED;
            case ADDED -> CacheEntryEventType.ADDED;
            case UPDATED -> CacheEntryEventType.UPDATED;
        };
    }
}
