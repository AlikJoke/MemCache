package ru.joke.memcache.core;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
abstract class EntryMetadata<T extends EntryMetadata<T, K>, K> implements Comparable<T> {

    protected final K key;
    protected final long expiredByLifespanAt;
    protected volatile long lastAccessed;

    protected EntryMetadata(@Nonnull K key, long expirationTimeout) {
        this.key = key;
        this.expiredByLifespanAt = expirationTimeout == -1 ? Long.MAX_VALUE : (System.currentTimeMillis() + expirationTimeout);
    }

    protected void onUsage() {
        this.lastAccessed = System.currentTimeMillis();
    }

    @Nonnull
    protected K key() {
        return key;
    }

    protected long expiredByLifespanAt() {
        return expiredByLifespanAt;
    }

    protected long lastAccessed() {
        return lastAccessed;
    }
}
