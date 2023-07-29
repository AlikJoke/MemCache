package ru.joke.memcache.core;

import javax.annotation.Nonnull;

abstract class EntryMetadata<T extends EntryMetadata<T, K>, K> implements Comparable<T> {

    protected final K key;
    protected final long expiredByTtlAt;
    protected volatile long lastAccessed;

    public EntryMetadata(
            @Nonnull K key,
            long expirationTimeout) {
        this.key = key;
        this.expiredByTtlAt = System.currentTimeMillis() + expirationTimeout;
    }

    public abstract void onUsage();

    public K key() {
        return key;
    }

    public long expiredByTtlAt() {
        return expiredByTtlAt;
    }

    public long lastAccessed() {
        return lastAccessed;
    }
}
