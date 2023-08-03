package ru.joke.memcache.core.stats;

import java.math.BigInteger;

public interface MemCacheStatistics {

    BigInteger approximateHitsCount();

    BigInteger approximateMissesCount();

    long readOnlyRetrievalHitsCount();

    long readOnlyRetrievalMissesCount();

    long expirationsCount();

    long evictionsCount();

    long removalMissesCount();

    long removalHitsCount();

    long putHitsCount();

    long putMissesCount();

    int currentEntriesCount();

    void reset();

    void setStatisticsEnabled(boolean statisticsEnabled);
}
