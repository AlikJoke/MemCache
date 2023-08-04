package ru.joke.memcache.core.stats;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.math.BigInteger;

public interface MemCacheStatistics {

    @Nonnull
    BigInteger approximateHitsCount();

    @Nonnull
    BigInteger approximateMissesCount();

    @Nonnegative
    long readOnlyRetrievalHitsCount();

    @Nonnegative
    long readOnlyRetrievalMissesCount();

    @Nonnegative
    long expirationsCount();

    @Nonnegative
    long evictionsCount();

    @Nonnegative
    long removalMissesCount();

    @Nonnegative
    long removalHitsCount();

    @Nonnegative
    long putHitsCount();

    @Nonnegative
    long putMissesCount();

    @Nonnegative
    int currentEntriesCount();

    void reset();

    void setStatisticsEnabled(boolean statisticsEnabled);
}
