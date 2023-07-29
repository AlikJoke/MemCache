package ru.joke.memcache.core.heap;

import javax.annotation.Nonnull;

final class MRUEntryMetadata<K> extends EntryMetadata<MRUEntryMetadata<K>, K> {

    public MRUEntryMetadata(@Nonnull K key, long expirationTimeout) {
        super(key, expirationTimeout);
    }

    @Override
    public void onUsage() {
        this.lastAccessed = System.currentTimeMillis();
    }

    @Override
    public int compareTo(MRUEntryMetadata<K> o) {
        return Long.compare(o.lastAccessed, this.lastAccessed);
    }
}