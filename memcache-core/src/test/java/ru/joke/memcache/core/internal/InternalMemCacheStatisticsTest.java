package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InternalMemCacheStatisticsTest {

    private static final int SIZE = 10;

    private InternalMemCacheStatistics statistics;

    @BeforeEach
    void setUp() {
        this.statistics = new InternalMemCacheStatistics(() -> SIZE);
    }

    @Test
    public void testHitsCountWhenStatsEnabled() {
        statistics.setStatisticsEnabled(true);
        statistics.onReadOnlyRetrievalHit();
        statistics.onReadOnlyRetrievalHit();

        statistics.onPutHit();
        statistics.onRemovalHit();
        statistics.onRemovalHit();

        final BigInteger expectedCount = BigInteger.valueOf(5);
        assertEquals(expectedCount, statistics.approximateHitsCount(), "Hits count must be equal");
        assertEquals(1, statistics.putHitsCount(), "Put hits count must be equal");
        assertEquals(2, statistics.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(2, statistics.readOnlyRetrievalHitsCount(), "Read only retrieval hits count must be equal");
    }

    @Test
    public void testHitsCountWhenStatsDisabled() {
        statistics.setStatisticsEnabled(false);
        statistics.onReadOnlyRetrievalHit();
        statistics.onPutHit();
        statistics.onRemovalHit();

        final BigInteger actualCount = statistics.approximateHitsCount();

        BigInteger expectedCount = BigInteger.ZERO;
        assertEquals(expectedCount, actualCount, "Hits count must be equal to zero when stats is disabled");
        assertEquals(0, statistics.putHitsCount(), "Put hits count must be equal");
        assertEquals(0, statistics.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(0, statistics.readOnlyRetrievalHitsCount(), "Read only retrieval hits count must be equal");
    }

    @Test
    public void testMissesCountWhenStatsEnabled() {
        statistics.setStatisticsEnabled(true);
        statistics.onReadOnlyRetrievalMiss();
        statistics.onReadOnlyRetrievalMiss();

        statistics.onPutMiss();
        statistics.onRemovalMiss();
        statistics.onRemovalMiss();

        final BigInteger expectedCount = BigInteger.valueOf(5);
        assertEquals(expectedCount, statistics.approximateMissesCount(), "Misses count must be equal");
        assertEquals(1, statistics.putMissesCount(), "Put misses count must be equal");
        assertEquals(2, statistics.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(2, statistics.readOnlyRetrievalMissesCount(), "Read only retrieval misses count must be equal");
    }

    @Test
    public void testMissesCountWhenStatsDisabled() {
        statistics.setStatisticsEnabled(false);
        statistics.onReadOnlyRetrievalMiss();
        statistics.onPutMiss();
        statistics.onRemovalMiss();

        final BigInteger actualCount = statistics.approximateMissesCount();

        BigInteger expectedCount = BigInteger.ZERO;
        assertEquals(expectedCount, actualCount, "Hits count must be equal to zero when stats is disabled");
        assertEquals(0, statistics.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, statistics.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(0, statistics.readOnlyRetrievalMissesCount(), "Read only retrieval misses count must be equal");
    }

    @Test
    public void testEntriesCount() {
        assertEquals(SIZE, statistics.currentEntriesCount(), "Entries count must be equal");
    }

    @Test
    public void testEvictionWhenStatsEnabled() {
        statistics.setStatisticsEnabled(true);

        statistics.onEviction();
        statistics.onEviction();

        assertEquals(2, statistics.evictionsCount(), "Evictions count must be equal");
    }

    @Test
    public void testEvictionWhenStatsDisabled() {
        statistics.setStatisticsEnabled(false);

        statistics.onEviction();
        statistics.onEviction();

        assertEquals(0, statistics.evictionsCount(), "Evictions count must be equal");
    }

    @Test
    public void testExpirationWhenStatsEnabled() {
        statistics.setStatisticsEnabled(true);

        statistics.onExpiration();
        statistics.onExpiration();

        assertEquals(2, statistics.expirationsCount(), "Expirations count must be equal");
    }

    @Test
    public void testExpirationWhenStatsDisabled() {
        statistics.setStatisticsEnabled(false);

        statistics.onExpiration();
        statistics.onExpiration();

        assertEquals(0, statistics.expirationsCount(), "Expirations count must be equal");
    }

    @Test
    public void testReset() {
        statistics.setStatisticsEnabled(true);

        statistics.onReadOnlyRetrievalHit();
        statistics.onPutHit();
        statistics.onRemovalHit();
        statistics.onEviction();
        statistics.onPutMiss();
        statistics.onExpiration();
        statistics.onRemovalMiss();
        statistics.onReadOnlyRetrievalMiss();
        statistics.onPutMiss();

        statistics.reset();

        assertEquals(0, statistics.approximateHitsCount().intValue(), "Count must be zero after reset");
        assertEquals(0, statistics.approximateMissesCount().intValue(), "Count must be zero after reset");
        assertEquals(0, statistics.readOnlyRetrievalHitsCount(), "Count must be zero after reset");
        assertEquals(0, statistics.readOnlyRetrievalMissesCount(), "Count must be zero after reset");
        assertEquals(0, statistics.putHitsCount(), "Count must be zero after reset");
        assertEquals(0, statistics.putMissesCount(), "Count must be zero after reset");
        assertEquals(0, statistics.removalHitsCount(), "Count must be zero after reset");
        assertEquals(0, statistics.removalMissesCount(), "Count must be zero after reset");
        assertEquals(0, statistics.expirationsCount(), "Count must be zero after reset");
        assertEquals(0, statistics.evictionsCount(), "Count must be zero after reset");
    }
}
