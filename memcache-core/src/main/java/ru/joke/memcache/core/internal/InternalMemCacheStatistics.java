package ru.joke.memcache.core.internal;

import ru.joke.memcache.core.stats.MemCacheStatistics;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;

final class InternalMemCacheStatistics implements MemCacheStatistics {

    private final AtomicLong hitsCount = new AtomicLong(0);
    private final AtomicLong missesCount = new AtomicLong(0);
    private final AtomicLong readOnlyRetrievalHits = new AtomicLong(0);
    private final AtomicLong readOnlyRetrievalMisses = new AtomicLong(0);
    private final AtomicLong removalHits = new AtomicLong(0);
    private final AtomicLong removalMisses = new AtomicLong(0);
    private final AtomicLong putHits = new AtomicLong(0);
    private final AtomicLong putMisses = new AtomicLong(0);
    private final AtomicLong expirationsCount = new AtomicLong(0);
    private final AtomicLong evictionsCount = new AtomicLong(0);
    private final IntSupplier cacheCountSupplier;

    private volatile boolean enabled;
    private volatile boolean resetInProgress;

    InternalMemCacheStatistics(@Nonnull IntSupplier cacheCountSupplier) {
        this.cacheCountSupplier = cacheCountSupplier;
    }

    @Override
    @Nonnull
    public BigInteger approximateHitsCount() {
        if (!this.enabled) {
            return BigInteger.ZERO;
        }

        while (this.resetInProgress);

        final BigInteger readOnlyRetrievalHits = BigInteger.valueOf(this.readOnlyRetrievalHits.get());
        final BigInteger putHits = BigInteger.valueOf(this.putHits.get());
        final BigInteger removalHits = BigInteger.valueOf(this.removalHits.get());

        return readOnlyRetrievalHits.add(putHits).add(removalHits);
    }

    @Override
    @Nonnull
    public BigInteger approximateMissesCount() {
        if (!this.enabled) {
            return BigInteger.ZERO;
        }

        while (this.resetInProgress);

        final BigInteger readOnlyRetrievalMisses = BigInteger.valueOf(this.readOnlyRetrievalMisses.get());
        final BigInteger putMisses = BigInteger.valueOf(this.putMisses.get());
        final BigInteger removalMisses = BigInteger.valueOf(this.removalMisses.get());

        return readOnlyRetrievalMisses.add(putMisses).add(removalMisses);
    }

    @Override
    @Nonnegative
    public long readOnlyRetrievalHitsCount() {
        return this.readOnlyRetrievalHits.get();
    }

    @Override
    @Nonnegative
    public long readOnlyRetrievalMissesCount() {
        return this.readOnlyRetrievalMisses.get();
    }

    @Override
    @Nonnegative
    public long expirationsCount() {
        return this.expirationsCount.get();
    }

    @Override
    @Nonnegative
    public long evictionsCount() {
        return this.evictionsCount.get();
    }

    @Override
    @Nonnegative
    public long removalMissesCount() {
        return this.removalMisses.get();
    }

    @Override
    @Nonnegative
    public long removalHitsCount() {
        return this.removalHits.get();
    }

    @Override
    @Nonnegative
    public long putHitsCount() {
        return this.putHits.get();
    }

    @Override
    @Nonnegative
    public long putMissesCount() {
        return this.putMisses.get();
    }

    @Override
    @Nonnegative
    public int currentEntriesCount() {
        return this.cacheCountSupplier.getAsInt();
    }

    @Override
    public synchronized void reset() {

        if (!this.enabled) {
            return;
        }

        this.resetInProgress = true;

        try {
            this.removalMisses.set(0);
            this.removalHits.set(0);
            this.readOnlyRetrievalMisses.set(0);
            this.readOnlyRetrievalHits.set(0);
            this.expirationsCount.set(0);
            this.putHits.set(0);
            this.putMisses.set(0);
            this.hitsCount.set(0);
            this.evictionsCount.set(0);
        } finally {
            this.resetInProgress = false;
        }
    }

    @Override
    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.enabled = statisticsEnabled;
    }

    void onRemovalMiss() {
        if (!this.enabled) {
            return;
        }

        while (this.resetInProgress);

        this.removalMisses.incrementAndGet();
    }

    void onRemovalHit() {
        if (!this.enabled) {
            return;
        }

        while (this.resetInProgress);

        this.removalHits.incrementAndGet();
    }

    void onExpiration() {
        if (!this.enabled) {
            return;
        }

        this.expirationsCount.incrementAndGet();
    }

    void onEviction() {
        if (!this.enabled) {
            return;
        }

        this.evictionsCount.incrementAndGet();
    }

    void onPutHit() {
        if (!this.enabled) {
            return;
        }

        while (this.resetInProgress);

        this.putHits.incrementAndGet();
    }

    void onPutMiss() {
        if (!this.enabled) {
            return;
        }

        while (this.resetInProgress);

        this.putMisses.incrementAndGet();
    }

    void onReadOnlyRetrievalHit() {
        if (!this.enabled) {
            return;
        }

        while (this.resetInProgress);

        this.readOnlyRetrievalHits.incrementAndGet();
    }

    void onReadOnlyRetrievalMiss() {
        if (!this.enabled) {
            return;
        }

        while (this.resetInProgress);

        this.readOnlyRetrievalMisses.incrementAndGet();
    }
}
