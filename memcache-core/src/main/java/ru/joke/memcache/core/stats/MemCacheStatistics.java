package ru.joke.memcache.core.stats;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.math.BigInteger;

/**
 * MemCache cache statistics.
 *
 * @author Alik
 * @see ru.joke.memcache.core.MemCache
 */
public interface MemCacheStatistics {

    /**
     * Returns the approximate number of cache hits (during read, delete, add operations).
     *
     * @return the approximate number of cache hits, cannot be {@code null}.
     */
    @Nonnull
    BigInteger approximateHitsCount();

    /**
     * Returns the approximate number of cache misses (during read, delete, add operations).
     *
     * @return the approximate number of cache misses, cannot be {@code null}.
     */
    @Nonnull
    BigInteger approximateMissesCount();

    /**
     * Returns the number of cache hits during read operations.
     *
     * @return the number of cache hits during read operations, cannot be negative.
     */
    @Nonnegative
    long readOnlyRetrievalHitsCount();

    /**
     * Returns the number of cache misses during read operations.
     *
     * @return the number of cache misses during read operations, cannot be negative.
     */
    @Nonnegative
    long readOnlyRetrievalMissesCount();

    /**
     * Returns the number of expired cache elements deleted.
     *
     * @return the number of expired cache elements deleted, cannot be negative.
     */
    @Nonnegative
    long expirationsCount();

    /**
     * Returns the number of cache elements evicted based on the eviction policy.
     *
     * @return the number of cache elements evicted, cannot be negative.
     */
    @Nonnegative
    long evictionsCount();

    /**
     * Returns the number of cache hits during delete operations.
     *
     * @return the number of cache hits during delete operations, cannot be negative.
     */
    @Nonnegative
    long removalMissesCount();

    /**
     * Returns the number of cache misses during delete operations.
     *
     * @return the number of cache misses during delete operations, cannot be negative.
     */
    @Nonnegative
    long removalHitsCount();

    /**
     * Returns the number of cache hits during add operations.
     *
     * @return the number of cache hits during add operations, cannot be negative.
     */
    @Nonnegative
    long putHitsCount();

    /**
     * Returns the number of cache misses during add operations.
     *
     * @return the number of cache misses during add operations, cannot be negative.
     */
    @Nonnegative
    long putMissesCount();

    /**
     * Returns the number of cache elements currently stored.
     *
     * @return the number of cache elements currently stored, cannot be negative.
     */
    @Nonnegative
    int currentEntriesCount();

    /**
     * Resets the current cache statistics.
     */
    void reset();

    /**
     * Enables or disables cache statistics tracking.
     *
     * @param statisticsEnabled statistics usage flag.
     */
    void setStatisticsEnabled(boolean statisticsEnabled);
}
