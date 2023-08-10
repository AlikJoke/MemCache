package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.ExpirationConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;
import ru.joke.memcache.core.configuration.PersistentStoreConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DiskPersistentCacheRepositoryTest {

    private EntryMetadataFactory metadataFactory;
    private DiskPersistentCacheRepository repository;
    private String storePath;

    @BeforeEach
    void setUp() {
        final String uid = "1";
        final String location = "${user.home}/tst";
        final String cacheName = "test";
        this.storePath = System.getProperty("user.home") + "/tst/" + uid + "/" + cacheName + ".bin";
        final var cacheConfiguration = CacheConfiguration
                                        .builder()
                                            .setCacheName("test")
                                            .setEvictionPolicy(CacheConfiguration.EvictionPolicy.LFU)
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
                                                                .setEternal(true)
                                                            .build()
                                            )
                                            .setPersistentStoreConfiguration(
                                                    PersistentStoreConfiguration
                                                            .builder()
                                                                .setUid(uid)
                                                                .setLocation(location)
                                                            .build()
                                            )
                                        .build();
        this.metadataFactory = new EntryMetadataFactory(cacheConfiguration);
        this.repository = new DiskPersistentCacheRepository(cacheConfiguration, this.metadataFactory);

        final File store = new File(this.storePath);
        store.delete();
    }

    @Test
    public void testLoadFromEmptyRepository() throws IOException {
        final File store = new File(this.storePath);
        store.mkdirs();
        store.createNewFile();

        try {
            assertTrue(store.exists(), "File store must exist after creation");

            final var result = this.repository.load();
            assertTrue(result.isEmpty(), "Entries must be empty when file store does not exist");
        } finally {
            store.delete();
        }
    }

    @Test
    public void testLoadFromNonExistenceRepository() {

        final File store = new File(this.storePath);
        assertFalse(store.exists(), "File store must not exist");

        final var result = this.repository.load();
        assertTrue(result.isEmpty(), "Entries must be empty when file store does not exist");
    }

    @Test
    public void testSaveAndRestore() {
        final File store = new File(this.storePath);
        assertFalse(store.exists(), "File store must not exist");

        final MemCacheEntry<Integer, ArrayList<String>> entry1 = new MemCacheEntry<>(new ArrayList<>(List.of("1")), this.metadataFactory.create(1));
        final MemCacheEntry<Integer, ArrayList<String>> entry2 = new MemCacheEntry<>(new ArrayList<>(List.of("1", "2")), this.metadataFactory.create(2));
        final MemCacheEntry<Integer, ArrayList<String>> entry3 = new MemCacheEntry<>(new ArrayList<>(), this.metadataFactory.create(3));

        final Set<MemCacheEntry<Integer, ArrayList<String>>> entries = new HashSet<>();
        entries.add(entry1);
        entries.add(entry2);
        entries.add(entry3);
        this.repository.save(entries);

        assertTrue(store.exists(), "File store must exist after saving");
        assertTrue(store.length() > 0, "File store must be not empty");

        final Collection<MemCacheEntry<Integer, ArrayList<String>>> restoredColl = this.repository.load();
        assertFalse(store.exists(), "File store must not exist after restore");
        assertEquals(entries.size(), restoredColl.size(), "Entries size after restore must be equal to the original entries size");

        final List<MemCacheEntry<Integer, ArrayList<String>>> restoredEntriesList = new ArrayList<>(restoredColl);
        final List<MemCacheEntry<Integer, ArrayList<String>>> originalEntriesList = new ArrayList<>(entries);

        for (int i = 0; i < entries.size(); i++) {
            final var entry = originalEntriesList.get(i);
            final var restoredEntry = restoredEntriesList.get(i);
            assertEquals(entry.value(), restoredEntry.value(), "Entry value after restore must be equal to original");
            assertEquals(entry.metadata().key(), restoredEntry.metadata().key(), "Entry key after restore must be equal to original");
        }
    }
}
