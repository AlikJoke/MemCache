package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NoPersistentCacheRepositoryTest {

    @Test
    public void testLoadEmpty() {
        final PersistentCacheRepository repository = new PersistentCacheRepository.NoPersistentCacheRepository();
        assertTrue(repository.load().isEmpty(), "Repository must be empty");
    }

    @Test
    public void testLoadEmptyAfterSave() {
        final PersistentCacheRepository repository = new PersistentCacheRepository.NoPersistentCacheRepository();
        repository.save(List.of(new MemCacheEntry<>("12", new EntryMetadataFactory.FIFOEntryMetadata<>(1, 10))));

        assertTrue(repository.load().isEmpty(), "Repository must be empty after save");
    }
}
