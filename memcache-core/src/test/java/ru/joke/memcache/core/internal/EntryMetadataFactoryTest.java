package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.ExpirationConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;

import static org.junit.jupiter.api.Assertions.*;

public class EntryMetadataFactoryTest {

    @Test
    public void testLFU() throws InterruptedException {
        final EntryMetadataFactory metadataFactory = createFactoryWithPolicy(CacheConfiguration.EvictionPolicy.LFU);
        final Integer key = 1;
        final EntryMetadataFactory.LFUEntryMetadata<Integer> metadata = metadataFactory.create(key);

        makeCommonChecks(metadata, key);

        final Integer keyToCompare = 2;
        final EntryMetadataFactory.LFUEntryMetadata<Integer> metadataToCompare = metadataFactory.create(keyToCompare);

        assertTrue(metadata.compareTo(metadataToCompare) > 0, "Used entry must be larger than non-used entry");

        metadataToCompare.onUsage();
        assertEquals(0, metadata.compareTo(metadataToCompare), "Entries must be equal with equal count of usages");

        metadataToCompare.onUsage();
        assertTrue(metadata.compareTo(metadataToCompare) < 0, "Used entry must be larger than entry with lesser count of usages");
    }

    @Test
    public void testLRU() throws InterruptedException {
        final EntryMetadataFactory metadataFactory = createFactoryWithPolicy(CacheConfiguration.EvictionPolicy.LRU);
        final Integer key = 1;
        final EntryMetadataFactory.LRUEntryMetadata<Integer> metadata = metadataFactory.create(key);

        makeCommonChecks(metadata, key);

        final Integer keyToCompare = 2;
        final EntryMetadataFactory.LRUEntryMetadata<Integer> metadataToCompare = metadataFactory.create(keyToCompare);

        assertTrue(metadata.compareTo(metadataToCompare) < 0, "Last entry must be larger");

        metadataToCompare.onUsage();
        assertTrue(metadata.compareTo(metadataToCompare) < 0, "Last used entry must be larger");
        Thread.sleep(1);

        metadata.onUsage();
        assertTrue(metadata.compareTo(metadataToCompare) > 0, "Last used entry must be larger");
    }

    @Test
    public void testMRU() throws InterruptedException {
        final EntryMetadataFactory metadataFactory = createFactoryWithPolicy(CacheConfiguration.EvictionPolicy.MRU);
        final Integer key = 1;
        final EntryMetadataFactory.MRUEntryMetadata<Integer> metadata = metadataFactory.create(key);

        makeCommonChecks(metadata, key);

        final Integer keyToCompare = 2;
        final EntryMetadataFactory.MRUEntryMetadata<Integer> metadataToCompare = metadataFactory.create(keyToCompare);

        assertTrue(metadata.compareTo(metadataToCompare) > 0, "Entry with larger last accessed time must be larger");

        Thread.sleep(1);
        metadata.onUsage();
        assertTrue(metadata.compareTo(metadataToCompare) < 0, "Entry with larger last accessed time must be larger");
    }

    @Test
    public void testFIFO() throws InterruptedException {
        final EntryMetadataFactory metadataFactory = createFactoryWithPolicy(CacheConfiguration.EvictionPolicy.FIFO);
        final Integer key = 1;
        final EntryMetadataFactory.FIFOEntryMetadata<Integer> metadata = metadataFactory.create(key);

        makeCommonChecks(metadata, key);

        final Integer keyToCompare = 2;
        final EntryMetadataFactory.FIFOEntryMetadata<Integer> metadataToCompare = metadataFactory.create(keyToCompare);

        assertTrue(metadata.compareTo(metadataToCompare) < 0, "Older entry must be lesser than new entry");
        metadataToCompare.onUsage();
        assertTrue(metadata.compareTo(metadataToCompare) < 0, "Older entry must be lesser than new entry; should not changed after usage");
    }

    @Test
    public void testLIFO() throws InterruptedException {
        final EntryMetadataFactory metadataFactory = createFactoryWithPolicy(CacheConfiguration.EvictionPolicy.LIFO);
        final Integer key = 1;
        final EntryMetadataFactory.LIFOEntryMetadata<Integer> metadata = metadataFactory.create(key);

        makeCommonChecks(metadata, key);

        final Integer keyToCompare = 2;
        final EntryMetadataFactory.LIFOEntryMetadata<Integer> metadataToCompare = metadataFactory.create(keyToCompare);

        assertTrue(metadata.compareTo(metadataToCompare) > 0, "Older entry must be lesser than new entry");
        metadataToCompare.onUsage();
        assertTrue(metadata.compareTo(metadataToCompare) > 0, "Older entry must be lesser than new entry; should not changed after usage");
    }

    private <T extends EntryMetadata<T, Integer>> void makeComparisonChecks(T metadata1, T metadata2) {

        assertTrue(metadata1.compareTo(metadata2) > 0, "Used entry must be larger than non-used entry");

        metadata2.onUsage();
        assertEquals(0, metadata1.compareTo(metadata2), "Entries must be equal with equal count of usages");

        metadata2.onUsage();
        assertTrue(metadata1.compareTo(metadata2) < 0, "Used entry must be larger than entry with lesser count of usages");
    }

    private void makeCommonChecks(EntryMetadata<?, Integer> metadata, Integer key) throws InterruptedException {

        assertNotNull(metadata, "Metadata must be not null");
        assertEquals(key, metadata.key(), "Key must be equal");
        Thread.sleep(1);
        assertTrue(metadata.expiredByLifespanAt() > System.currentTimeMillis(), "Key must be equal");

        final long lastAccessed = metadata.lastAccessed();
        Thread.sleep(1);
        metadata.onUsage();
        Thread.sleep(1);

        assertTrue(metadata.lastAccessed() > lastAccessed, "Last accessed time must be more than prev last accessed time after usage");
    }

    private EntryMetadataFactory createFactoryWithPolicy(final CacheConfiguration.EvictionPolicy policy) {
        final var configuration =
                CacheConfiguration
                        .builder()
                            .setCacheName("test")
                            .setEvictionPolicy(policy)
                            .setMemoryStoreConfiguration(
                                    MemoryStoreConfiguration
                                            .builder()
                                                .setMaxEntries(10)
                                                .setConcurrencyLevel(1)
                                            .build()
                            )
                            .setExpirationConfiguration(
                                    ExpirationConfiguration
                                            .builder()
                                                .setLifespan(1000)
                                            .build()
                            )
                        .build();
        return new EntryMetadataFactory(configuration);
    }
}
