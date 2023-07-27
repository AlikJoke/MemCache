package ru.joke.memcache.core.heap;

import javax.annotation.Nonnull;

public abstract class EntryMetadata<T extends EntryMetadata<T, K>, K> implements Comparable<T> {

    protected final K key;
    protected final long expiredByTTLAt;
    protected volatile long lastAccessed;

    public EntryMetadata(
            @Nonnull K key,
            long expirationTimeout) {
        this.key = key;
        this.expiredByTTLAt = System.currentTimeMillis() + expirationTimeout;
    }

    public abstract void onUsage();
}
