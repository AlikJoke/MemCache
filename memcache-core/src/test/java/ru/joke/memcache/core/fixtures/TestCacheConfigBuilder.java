package ru.joke.memcache.core.fixtures;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.ExpirationConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;
import ru.joke.memcache.core.configuration.PersistentStoreConfiguration;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import java.util.List;

public class TestCacheConfigBuilder {

    public static CacheConfiguration build(
            final String cacheName,
            final CacheConfiguration.EvictionPolicy policy,
            final int maxEntries,
            final int concurrencyLevel,
            final String storageUid,
            final String location,
            final boolean eternal,
            final long lifespan,
            final long idleTimeout,
            final List<CacheEntryEventListener<?, ?>> listeners) {
        final var persistentStoreConfig =
                storageUid == null
                        ? null
                        : PersistentStoreConfiguration
                            .builder()
                                .setUid(storageUid)
                                .setLocation(location)
                            .build();
        return CacheConfiguration
                .builder()
                    .setCacheName(cacheName)
                    .setEvictionPolicy(policy)
                    .setMemoryStoreConfiguration(
                            MemoryStoreConfiguration
                                    .builder()
                                        .setMaxEntries(maxEntries)
                                        .setConcurrencyLevel(concurrencyLevel)
                                    .build()
                    )
                    .setPersistentStoreConfiguration(persistentStoreConfig)
                    .setExpirationConfiguration(
                            ExpirationConfiguration
                                    .builder()
                                        .setEternal(eternal)
                                        .setLifespan(lifespan)
                                        .setIdleTimeout(idleTimeout)
                                    .build()
                    )
                    .setCacheEntryEventListeners(listeners)
                .build();
    }
}
