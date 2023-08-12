package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.Lifecycle;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.events.CacheEntriesEvent;
import ru.joke.memcache.core.events.CacheEntryEvent;
import ru.joke.memcache.core.events.CacheEntryEventListener;
import ru.joke.memcache.core.events.EventType;
import ru.joke.memcache.core.fixtures.TestCacheConfigBuilder;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class MapMemCacheTest {

    private static final String CACHE_NAME = "test";

    private AsyncOpsInvoker asyncOpsInvoker;

    @BeforeEach
    void setUp() {
        this.asyncOpsInvoker = new AsyncOpsInvoker(1);
    }

    @Test
    public void testInstantiatedCache() {
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, true, -1, -1, Collections.emptyList());
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));

        assertEquals(CACHE_NAME, cache.name(), "Cache name must be equal");
        assertEquals(cacheConfig, cache.configuration(), "Cache configuration must be equal");
        assertEquals(Lifecycle.ComponentStatus.UNAVAILABLE, cache.status(), "Cache status must be " + Lifecycle.ComponentStatus.UNAVAILABLE);
    }

    @Test
    public void testCacheInitializationWithEmptyPersistentStore() {
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, true, -1, -1, Collections.emptyList());
        final EntryMetadataFactory metadataFactory = new EntryMetadataFactory(cacheConfig);
        final MemCacheEntry<Integer, String> entry1 = new MemCacheEntry<>("1", metadataFactory.create(1));
        final MemCacheEntry<Integer, String> entry2 = new MemCacheEntry<>("2", metadataFactory.create(2));

        final var repositorySpy = new PersistentCacheRepositorySpy(Set.of(entry1, entry2));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, repositorySpy, metadataFactory);
        cache.initialize();

        assertEquals(Lifecycle.ComponentStatus.RUNNING, cache.status(), "Cache status must be " + Lifecycle.ComponentStatus.RUNNING + " after initialization");
        assertTrue(repositorySpy.restoreWasCalled, "Restore should be called");
        assertTrue(cache.get(entry1.metadata().key()).filter(entry1.value()::equals).isPresent(), "Entry must present in cache after restore");
        assertTrue(cache.get(entry2.metadata().key()).filter(entry2.value()::equals).isPresent(), "Entry must present in cache after restore");
    }

    @Test
    public void testCacheInitializationWithErrorPersistentStore() {
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, true, -1, -1, Collections.emptyList());
        final var errorRepository = new ErrorOnRestoreRepository();
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, errorRepository, new EntryMetadataFactory(cacheConfig));
        assertThrows(RuntimeException.class, cache::initialize, "Exception must be thrown on restore from repository");

        assertEquals(Lifecycle.ComponentStatus.FAILED, cache.status(), "Cache status must be " + Lifecycle.ComponentStatus.FAILED + " after initialization with error repository");
    }

    @Test
    public void testPutToCache() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 2, 1, null, null, true, -1, -1, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "1!";
        assertTrue(cache.put(1, v1).isEmpty(), "Old value must not present");
        assertTrue(cache.get(1).filter(v -> v.equals(v1)).isPresent(), "Added value must present");
        assertTrue(cache.put(1, v1_updated).filter(v -> v.equals(v1)).isPresent(), "Old value must present");

        final String v2 = "2";
        assertTrue(cache.put(2, v2).isEmpty(), "Old value must not present");

        final String v3 = "3";
        assertTrue(cache.put(3, v3).isEmpty(), "Old value must not present");
        assertTrue(cache.get(1).isEmpty(), "First added value must be evicted by policy");

        assertTrue(cache.put(3, null).filter(v -> v.equals(v3)).isPresent(), "Old value must present");

        assertTrue(cache.get(2).filter(v -> v.equals(v2)).isPresent(), "Added value must present");
        assertTrue(cache.get(3).isEmpty(), "Value must not present after null put");

        assertEquals(1, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(1, stats.evictionsCount(), "Evictions count must be equal");
        assertEquals(1, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(4, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(2, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(2, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(2, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(7, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(6, listener.events.size(), "Events count must be equal");

        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeUpdatedEventChecks(1, v1, v1_updated, listener.events.get(1));
        makeAddedEventChecks(2, v2, listener.events.get(2));
        makeRemovedEventChecks(1, v1_updated, listener.events.get(3));
        makeAddedEventChecks(3, v3, listener.events.get(4));
        makeRemovedEventChecks(3, v3, listener.events.get(5));
    }

    @Test
    public void testPutIfAbsentToCache() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 2, 1, null, null, true, -1, -1, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "1!";
        assertTrue(cache.putIfAbsent(1, v1).isEmpty(), "Old value must not present");
        assertTrue(cache.putIfAbsent(1, v1_updated).filter(v -> v.equals(v1)).isPresent(), "Added value must present");

        final String v2 = "2";
        assertTrue(cache.put(2, v2).isEmpty(), "Old value must not present");

        assertTrue(cache.get(1).filter(v -> v.equals(v1)).isPresent(), "First added value must present");
        assertTrue(cache.get(2).filter(v -> v.equals(v2)).isPresent(), "Added value must present");
        
        assertEquals(2, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(2, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(1, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(2, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(1, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(4, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(2, listener.events.size(), "Events count must be equal");

        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeAddedEventChecks(2, v2, listener.events.get(1));
    }

    @Test
    public void testRemoveFromCache() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 2, 1, null, null, true, -1, -1, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        assertTrue(cache.put(1, v1).isEmpty(), "Old value must not present");

        final String v2 = "2";
        assertTrue(cache.put(2, v2).isEmpty(), "Old value must not present");

        assertTrue(cache.remove(1).filter(v -> v.equals(v1)).isPresent(), "Removed value must present");
        assertFalse(cache.remove(2, v1), "Entry must not be removed (another value)");
        assertTrue(cache.remove(2, v2), "Entry must be removed");

        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(2, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(0, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(1, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(2, stats.removalHitsCount(), "Removal hits count must be equal");

        assertEquals(1, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(4, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(4, listener.events.size(), "Events count must be equal");

        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeAddedEventChecks(2, v2, listener.events.get(1));
        makeRemovedEventChecks(1, v1, listener.events.get(2));
        makeRemovedEventChecks(2, v2, listener.events.get(3));
    }

    @Test
    public void testReplaceInCache() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, true, -1, -1, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        assertTrue(cache.put(1, v1).isEmpty(), "Old value must not present");

        final String v2 = "2";
        assertTrue(cache.put(2, v2).isEmpty(), "Old value must not present");

        assertTrue(cache.replace(1, v1, v2), "Value must be replaced");
        assertFalse(cache.replace(2, v1, v2), "Value must not be replaced");

        final String v3 = "3";
        assertTrue(cache.replace(3, null, v3), "Value must be replaced (added)");
        assertTrue(cache.replace(3, v3, null), "Value must be replaced (removed)");

        assertEquals(2, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(4, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(1, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(0, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(1, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(1, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(5, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(5, listener.events.size(), "Events count must be equal");

        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeAddedEventChecks(2, v2, listener.events.get(1));
        makeUpdatedEventChecks(1, v1, v2, listener.events.get(2));
        makeAddedEventChecks(3, v3, listener.events.get(3));
        makeRemovedEventChecks(3, v3, listener.events.get(4));
    }

    @Test
    public void testClearCache() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, true, -1, -1, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        assertTrue(cache.put(1, v1).isEmpty(), "Old value must not present");

        final String v2 = "2";
        assertTrue(cache.put(2, v2).isEmpty(), "Old value must not present");

        cache.clear();

        assertTrue(cache.get(1).isEmpty(), "Value must not present");
        assertTrue(cache.get(2).isEmpty(), "Value must not present");

        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(2, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(0, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(2, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(0, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(0, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(2, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(2, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(2, listener.events.size(), "Events count must be equal");
        assertEquals(1, listener.batchEvents.size(), "Batch events count must be equal");

        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeAddedEventChecks(2, v2, listener.events.get(1));

        assertEquals(EventType.REMOVED, listener.batchEvents.get(0).eventType(), "Event type must be equal");
    }

    @Test
    public void testClearExpired() throws InterruptedException {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, false, 100, 60, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        assertTrue(cache.put(1, v1).isEmpty(), "Old value must not present");

        final String v2 = "2";
        assertTrue(cache.put(2, v2).isEmpty(), "Old value must not present");

        final String v3 = "3";
        assertTrue(cache.put(3, v3).isEmpty(), "Old value must not present");

        Thread.sleep(20);
        cache.clearExpired();

        assertEquals(3, stats.currentEntriesCount(), "Current entries count must be equal");
        assertTrue(cache.get(1).isPresent(), "Value must present");

        Thread.sleep(40);
        cache.clearExpired();

        assertEquals(2, stats.expirationsCount(), "Current entries count must be equal");
        assertEquals(1, stats.currentEntriesCount(), "Current entries count must be equal");
        assertTrue(cache.get(1).isPresent(), "Value must present");
        assertTrue(cache.get(2).isEmpty(), "Value must not present (expired by idle timeout)");

        Thread.sleep(40);
        cache.clearExpired();

        assertEquals(3, stats.expirationsCount(), "Current entries count must be equal");
        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertTrue(cache.get(1).isEmpty(), "Value must not present (expired by lifespan)");

        assertEquals(6, listener.events.size(), "Events count must be equal");
        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeAddedEventChecks(2, v2, listener.events.get(1));
        makeAddedEventChecks(3, v3, listener.events.get(2));
        makeExpiredEventChecks(2, v2, listener.events.get(3));
        makeExpiredEventChecks(3, v3, listener.events.get(4));
        makeExpiredEventChecks(1, v1, listener.events.get(5));
    }

    @Test
    public void testMergeOperation() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, false, 100, 60, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "2";
        assertTrue(cache.merge(1, v1, (k, v) -> v1_updated).filter(v1::equals).isPresent(), "New value must be equal (added)");
        assertTrue(cache.merge(1, v1, (k, v) -> v1_updated).filter(v1_updated::equals).isPresent(), "New value must be equal (update)");
        assertTrue(cache.merge(1, v1, (k, v) -> v1_updated).filter(v1_updated::equals).isPresent(), "New value must be equal (without update)");
        assertTrue(cache.merge(1, v1, (k, v) -> null).isEmpty(), "New value must not present (removed)");

        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(2, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(1, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(0, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(0, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(1, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(1, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(3, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(3, listener.events.size(), "Events count must be equal");
        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeUpdatedEventChecks(1, v1, v1_updated, listener.events.get(1));
        makeRemovedEventChecks(1, v1_updated, listener.events.get(2));
    }

    @Test
    public void testComputeIfAbsentOperation() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, false, 100, 60, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "2";
        assertTrue(cache.computeIfAbsent(1, k -> v1).filter(v1::equals).isPresent(), "New value must be equal (added)");
        assertTrue(cache.computeIfAbsent(1, k -> v1_updated).filter(v1::equals).isPresent(), "New value must be equal (without update)");

        assertEquals(1, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(1, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(0, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(1, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(0, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(0, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(0, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(0, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(2, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(1, listener.events.size(), "Events count must be equal");
        makeAddedEventChecks(1, v1, listener.events.get(0));
    }

    @Test
    public void testComputeIfPresentOperation() {

        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, false, 100, 60, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "2";
        assertTrue(cache.computeIfPresent(1, (k, v) -> v1).isEmpty(), "New value must be not present (old value is not present)");
        cache.put(1, v1);
        assertTrue(cache.computeIfPresent(1, (k, v) -> v1_updated).filter(v1_updated::equals).isPresent(), "New value must be equal");
        assertTrue(cache.computeIfPresent(1, (k, v) -> null).isEmpty(), "New value must not present");

        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(2, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(0, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(0, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(0, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(1, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(0, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(3, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(3, listener.events.size(), "Events count must be equal");
        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeUpdatedEventChecks(1, v1, v1_updated, listener.events.get(1));
        makeRemovedEventChecks(1, v1_updated, listener.events.get(2));
    }

    @Test
    public void testComputeOperation() {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 3, 1, null, null, false, 100, 60, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "2";
        assertTrue(cache.compute(1, (k, v) -> v == null ? v1 : null).filter(v1::equals).isPresent(), "New value must be equal (added)");
        assertTrue(cache.compute(1, (k, v) -> v.equals(v1) ? v1_updated : v).filter(v1_updated::equals).isPresent(), "New value must be equal (update)");
        assertTrue(cache.compute(1, (k, v) -> v.equals(v1_updated) ? null : v).isEmpty(), "New value must not present (removed)");

        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(2, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(0, stats.putMissesCount(), "Put misses count must be equal");
        assertEquals(0, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(0, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(0, stats.removalMissesCount(), "Removal misses count must be equal");
        assertEquals(1, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(0, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(3, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(3, listener.events.size(), "Events count must be equal");
        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeUpdatedEventChecks(1, v1, v1_updated, listener.events.get(1));
        makeRemovedEventChecks(1, v1_updated, listener.events.get(2));
    }

    @Test
    public void testAsyncCacheOps() throws ExecutionException, InterruptedException {
        final var listener = new ListenerSpy();
        final var cacheConfig = TestCacheConfigBuilder.build(CACHE_NAME, CacheConfiguration.EvictionPolicy.FIFO, 2, 1, null, null, true, -1, -1, List.of(listener));
        final var cache = new MapMemCache<>(cacheConfig, this.asyncOpsInvoker, new PersistentCacheRepository.NoPersistentCacheRepository(), new EntryMetadataFactory(cacheConfig));
        cache.initialize();

        final var stats = cache.statistics();
        stats.setStatisticsEnabled(true);

        final String v1 = "1";
        final String v1_updated = "1!";

        assertTrue(cache.putAsync(1, v1).get().isEmpty(), "Old value must not present");
        assertTrue(cache.getAsync(1).get().filter(v -> v.equals(v1)).isPresent(), "Added value must present");
        assertTrue(cache.putAsync(1, v1_updated).get().filter(v -> v.equals(v1)).isPresent(), "Old value must present");

        final String v2 = "2";
        assertTrue(cache.putAsync(2, v2).get().isEmpty(), "Old value must not present");

        final String v3 = "3";
        assertTrue(cache.putAsync(3, v3).get().isEmpty(), "Old value must not present");
        assertTrue(cache.getAsync(1).get().isEmpty(), "First added value must be evicted by policy");

        assertTrue(cache.putAsync(3, null).get().filter(v -> v.equals(v3)).isPresent(), "Old value must present");

        assertTrue(cache.getAsync(2).get().filter(v -> v.equals(v2)).isPresent(), "Added value must present");
        assertTrue(cache.getAsync(3).get().isEmpty(), "Value must not present after null put");

        cache.clearAsync().get();
        assertTrue(cache.getAsync(2).get().isEmpty(), "Value must not present after clear");

        assertEquals(0, stats.currentEntriesCount(), "Current entries count must be equal");
        assertEquals(1, stats.evictionsCount(), "Evictions count must be equal");
        assertEquals(1, stats.removalHitsCount(), "Removal hits count must be equal");
        assertEquals(4, stats.putHitsCount(), "Put hits count must be equal");
        assertEquals(2, stats.readOnlyRetrievalHitsCount(), "Read only retrievals hits count must be equal");
        assertEquals(3, stats.readOnlyRetrievalMissesCount(), "Read only retrievals misses count must be equal");
        assertEquals(3, stats.approximateMissesCount().intValue(), "Common misses count must be equal");
        assertEquals(7, stats.approximateHitsCount().intValue(), "Common hits count must be equal");

        assertEquals(6, listener.events.size(), "Events count must be equal");
        assertEquals(1, listener.batchEvents.size(), "Events count must be equal");

        makeAddedEventChecks(1, v1, listener.events.get(0));
        makeUpdatedEventChecks(1, v1, v1_updated, listener.events.get(1));
        makeAddedEventChecks(2, v2, listener.events.get(2));
        makeRemovedEventChecks(1, v1_updated, listener.events.get(3));
        makeAddedEventChecks(3, v3, listener.events.get(4));
        makeRemovedEventChecks(3, v3, listener.events.get(5));

        assertEquals(EventType.REMOVED, listener.batchEvents.get(0).eventType(), "Event type must be equal");
    }

    private void makeRemovedEventChecks(final Integer key, final String value, final CacheEntryEvent<?, ?> event) {

        assertEquals(key, event.key(), "Event key must be equal");
        assertEquals(EventType.REMOVED, event.eventType(), "Event type must be equal");
        assertTrue(event.oldValue().filter(v -> v.equals(value)).isPresent(), "Old value must be equal");
        assertTrue(event.newValue().isEmpty(), "New value must be empty");
    }

    private void makeExpiredEventChecks(final Integer key, final String value, final CacheEntryEvent<?, ?> event) {

        assertEquals(key, event.key(), "Event key must be equal");
        assertEquals(EventType.EXPIRED, event.eventType(), "Event type must be equal");
        assertTrue(event.oldValue().filter(v -> v.equals(value)).isPresent(), "Old value must be equal");
        assertTrue(event.newValue().isEmpty(), "New value must be empty");
    }

    private void makeUpdatedEventChecks(final Integer key, final String oldValue, final String newValue, final CacheEntryEvent<?, ?> event) {

        assertEquals(key, event.key(), "Event key must be equal");
        assertEquals(EventType.UPDATED, event.eventType(), "Event type must be equal");
        assertTrue(event.oldValue().filter(v -> v.equals(oldValue)).isPresent(), "Old value must be equal");
        assertTrue(event.newValue().filter(v -> v.equals(newValue)).isPresent(), "New value must be equal");
    }

    private void makeAddedEventChecks(final Integer key, final String value, final CacheEntryEvent<?, ?> event) {

        assertEquals(key, event.key(), "Event key must be equal");
        assertEquals(EventType.ADDED, event.eventType(), "Event type must be equal");
        assertTrue(event.oldValue().isEmpty(), "Old value must not present");
        assertTrue(event.newValue().filter(v -> v.equals(value)).isPresent(), "New value must be equal");
    }

    static class ListenerSpy implements CacheEntryEventListener<Serializable, Serializable> {

        private final List<CacheEntryEvent<?, ?>> events = new ArrayList<>();
        private final List<CacheEntriesEvent<?, ?>> batchEvents = new ArrayList<>();

        @Override
        public void onEvent(@Nonnull CacheEntryEvent<? extends Serializable, ? extends Serializable> event) {
            this.events.add(event);
        }

        @Override
        public void onBatchEvent(@Nonnull CacheEntriesEvent<? extends Serializable, ? extends Serializable> event) {
            this.batchEvents.add(event);
        }
    }

    static class ErrorOnRestoreRepository extends PersistentCacheRepository {

        @Nonnull
        @Override
        <K extends Serializable, V extends Serializable> Collection<MemCacheEntry<K, V>> load() {
            throw new RuntimeException();
        }

        @Override
        <K extends Serializable, V extends Serializable> void save(@Nonnull Collection<MemCacheEntry<K, V>> memCacheEntries) {

        }
    }

    static class PersistentCacheRepositorySpy extends PersistentCacheRepository {

        private final Set<MemCacheEntry<Integer, String>> entries;
        private boolean restoreWasCalled;

        PersistentCacheRepositorySpy(Set<MemCacheEntry<Integer, String>> entries) {
            this.entries = entries;
        }

        @Nonnull
        @Override
        <K extends Serializable, V extends Serializable> Collection<MemCacheEntry<K, V>> load() {
            this.restoreWasCalled = true;
            return entries
                    .stream()
                    .map(e -> (MemCacheEntry<K, V>) e)
                    .collect(Collectors.toSet());
        }

        @Override
        <K extends Serializable, V extends Serializable> void save(@Nonnull Collection<MemCacheEntry<K, V>> memCacheEntries) {

        }
    }
}
