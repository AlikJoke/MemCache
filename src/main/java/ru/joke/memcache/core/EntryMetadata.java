package ru.joke.memcache.core;

import javax.annotation.Nonnull;

abstract class EntryMetadata<T extends EntryMetadata<T, K>, K> implements Comparable<T> {

    protected final K key;
    protected final long expiredByTtlAt;
    protected volatile long lastAccessed;

    protected EntryMetadata(@Nonnull K key, long expirationTimeout) {
        this.key = key;
        this.expiredByTtlAt = System.currentTimeMillis() + expirationTimeout;
    }

    protected void onUsage() {
        this.lastAccessed = System.currentTimeMillis();
    }

    @Nonnull
    protected K key() {
        return key;
    }

    protected long expiredByTtlAt() {
        return expiredByTtlAt;
    }

    protected long lastAccessed() {
        return lastAccessed;
    }
}
