package ru.joke.memcache.core.internal;

import ru.joke.memcache.core.configuration.CacheConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
@Immutable
final class EntryMetadataFactory {

    private final CacheConfiguration.EvictionPolicy policy;
    private final long expirationTimeout;

    EntryMetadataFactory(@Nonnull CacheConfiguration configuration) {
        this.policy = configuration.evictionPolicy();
        this.expirationTimeout = configuration.expirationConfiguration().lifespan();
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
            case FIFO -> {
                @SuppressWarnings("unchecked")
                final T result = (T) new FIFOEntryMetadata<>(key, this.expirationTimeout);
                yield result;
            }
            case LIFO -> {
                @SuppressWarnings("unchecked")
                final T result = (T) new LIFOEntryMetadata<>(key, this.expirationTimeout);
                yield result;
            }
        };
    }

    @ThreadSafe
    static class FIFOEntryMetadata<K> extends EntryMetadata<FIFOEntryMetadata<K>, K> {

        FIFOEntryMetadata(@Nonnull K key, long expirationTimeout) {
            super(key, expirationTimeout);
        }

        @Override
        public int compareTo(FIFOEntryMetadata<K> o) {
            final int result = Long.compare(this.expiredByLifespanAt, o.expiredByLifespanAt);
            return result == 0 ? this.equals(o) ? 0 : (hashCode() > o.hashCode() ? 1 : -1) : result;
        }
    }

    @ThreadSafe
    static class LFUEntryMetadata<K> extends EntryMetadata<LFUEntryMetadata<K>, K>{

        private final AtomicLong usageCounter = new AtomicLong(0);

        LFUEntryMetadata(@Nonnull K key, long expirationTimeout) {
            super(key, expirationTimeout);
        }

        @Override
        public void onUsage() {
            super.onUsage();
            this.usageCounter.incrementAndGet();
        }

        @Override
        public int compareTo(LFUEntryMetadata<K> o) {
            final int result = Long.compare(this.usageCounter.get(), o.usageCounter.get());
            return result == 0 ? this.equals(o) ? 0 : (hashCode() > o.hashCode() ? 1 : -1) : result;
        }

        @Override
        protected void storeMetadata(ObjectOutput objectOutput) throws IOException {
            super.storeMetadata(objectOutput);
            objectOutput.writeLong(usageCounter.get());
        }

        @Override
        protected void restoreMetadata(ObjectInput objectInput) throws IOException {
            super.restoreMetadata(objectInput);
            this.usageCounter.set(objectInput.readLong());
        }
    }

    @ThreadSafe
    static class LIFOEntryMetadata<K> extends EntryMetadata<LIFOEntryMetadata<K>, K> {

        LIFOEntryMetadata(@Nonnull K key, long expirationTimeout) {
            super(key, expirationTimeout);
        }

        @Override
        public int compareTo(LIFOEntryMetadata<K> o) {
            final int result = Long.compare(o.expiredByLifespanAt, this.expiredByLifespanAt);
            return result == 0 ? this.equals(o) ? 0 : (hashCode() > o.hashCode() ? 1 : -1) : result;
        }
    }

    @ThreadSafe
    static class LRUEntryMetadata<K> extends EntryMetadata<LRUEntryMetadata<K>, K> {

        LRUEntryMetadata(@Nonnull K key, long expirationTimeout) {
            super(key, expirationTimeout);
        }

        @Override
        public int compareTo(LRUEntryMetadata<K> o) {
            final int result = Long.compare(this.lastAccessed, o.lastAccessed);
            return result == 0 ? this.equals(o) ? 0 : (hashCode() > o.hashCode() ? 1 : -1) : result;
        }
    }

    @ThreadSafe
    static class MRUEntryMetadata<K> extends EntryMetadata<MRUEntryMetadata<K>, K> {

        MRUEntryMetadata(@Nonnull K key, long expirationTimeout) {
            super(key, expirationTimeout);
        }

        @Override
        public int compareTo(MRUEntryMetadata<K> o) {
            final int result = Long.compare(o.lastAccessed, this.lastAccessed);
            return result == 0 ? this.equals(o) ? 0 : (hashCode() > o.hashCode() ? 1 : -1) : result;
        }
    }
}
