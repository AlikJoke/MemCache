package ru.joke.memcache.core.heap;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicLong;

public class LFUEntryMetadata<K> extends EntryMetadata<LFUEntryMetadata<K>, K>{

    private final AtomicLong usageCounter = new AtomicLong(0);

    public LFUEntryMetadata(
            @Nonnull K key,
            long expirationTimeout) {
        super(key, expirationTimeout);
    }

    @Override
    public void onUsage() {
        this.lastAccessed = System.currentTimeMillis();
        this.usageCounter.incrementAndGet();
    }

    @Override
    public int compareTo(LFUEntryMetadata<K> o) {
        return Long.compare(this.usageCounter.get(), o.usageCounter.get());
    }
}
