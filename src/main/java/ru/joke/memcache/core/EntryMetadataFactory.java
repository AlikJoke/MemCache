package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.EvictionPolicy;

import javax.annotation.Nonnull;

final class EntryMetadataFactory {

    private final EvictionPolicy policy;
    private final long expirationTimeout;

    EntryMetadataFactory(@Nonnull CacheConfiguration configuration) {
        this.policy = configuration.evictionPolicy();
        this.expirationTimeout = configuration.expirationConfiguration().expirationTimeout();
    }

    @Nonnull
    <T extends EntryMetadata<T, K>, K> T create(@Nonnull K key) {
        return switch (this.policy) {
            case LRU -> {
                @SuppressWarnings("unchecked")
                final T result = (T) new LRUEntryMetadata<>(key, this.expirationTimeout);
                yield result;
            }
            case LFU -> {
                @SuppressWarnings("unchecked")
                final T result = (T) new LFUEntryMetadata<>(key, this.expirationTimeout);
                yield result;
            }
            case MRU -> {
                @SuppressWarnings("unchecked")
                final T result = (T) new MRUEntryMetadata<>(key, this.expirationTimeout);
                yield result;
            }
        };
    }
}
