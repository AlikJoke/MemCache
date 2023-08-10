package ru.joke.memcache.core.internal;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@ThreadSafe
abstract class EntryMetadata<T extends EntryMetadata<T, K>, K> implements Comparable<T> {

    protected final K key;
    protected final long expiredByLifespanAt;
    protected volatile long lastAccessed;

    protected EntryMetadata(@Nonnull K key, long expirationTimeout) {
        this.key = key;
        this.lastAccessed = System.currentTimeMillis();
        this.expiredByLifespanAt = expirationTimeout == -1 ? Long.MAX_VALUE : (System.currentTimeMillis() + expirationTimeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EntryMetadata<?, ?> that = (EntryMetadata<?, ?>) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
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

    protected void storeMetadata(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeLong(System.currentTimeMillis() - this.lastAccessed);
    }

    protected void restoreMetadata(ObjectInput objectInput) throws IOException {
        // Always safe operation: reading and writing the field will not be performed at the same time in other threads
        final long lastAccessed = this.lastAccessed;
        this.lastAccessed = lastAccessed - objectInput.readLong();
    }
}
