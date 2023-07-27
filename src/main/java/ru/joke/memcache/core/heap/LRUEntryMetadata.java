package ru.joke.memcache.core.heap;

import javax.annotation.Nonnull;

public class LRUEntryMetadata<K> extends EntryMetadata<LRUEntryMetadata<K>, K> {

    public LRUEntryMetadata(@Nonnull K key, long expirationTimeout) {
        super(key, expirationTimeout);
    }

    @Override
    public void onUsage() {
        this.lastAccessed = System.currentTimeMillis();
    }

    @Override
    public int compareTo(LRUEntryMetadata<K> o) {
        return Long.compare(this.lastAccessed, o.lastAccessed);
    }
}