package ru.joke.memcache.core.heap;

import ru.joke.memcache.core.EvictionConfiguration;
import ru.joke.memcache.core.EvictionPolicy;

import javax.annotation.Nonnull;

public class EntryMetadataFactory {

    private final EvictionPolicy policy;
    private final long expirationTimeout;

    public EntryMetadataFactory(EvictionConfiguration configuration) {
        this.policy = configuration.evictionPolicy();
        this.expirationTimeout = configuration.expirationTimeout();
    }

    public <T extends EntryMetadata<T, K>, K> T create(@Nonnull K key) {
        if (this.policy == EvictionPolicy.LRU) {
            @SuppressWarnings("unchecked")
            final T result = (T) new LRUEntryMetadata<>(key, this.expirationTimeout);
            return result;
        } else {
            @SuppressWarnings("unchecked")
            final T result = (T) new LFUEntryMetadata<>(key, this.expirationTimeout);
            return result;
        }
    }
}
