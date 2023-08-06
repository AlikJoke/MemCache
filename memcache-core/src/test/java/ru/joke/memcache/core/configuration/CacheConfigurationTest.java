package ru.joke.memcache.core.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class CacheConfigurationTest {

    private MemoryStoreConfiguration memoryStoreConfig;
    private PersistentStoreConfiguration persistentStoreConfig;
    private CacheConfiguration.EvictionPolicy evictionPolicy;
    private ExpirationConfiguration expirationConfig;
    private CacheEntryEventListener<?, ?> listener;

    @BeforeEach
    public void setUp() {
        this.evictionPolicy = CacheConfiguration.EvictionPolicy.LRU;
        this.memoryStoreConfig = MemoryStoreConfiguration
                                            .builder()
                                                .setMaxEntries(1)
                                                .setConcurrencyLevel(1)
                                            .build();
        this.persistentStoreConfig = PersistentStoreConfiguration
                                        .builder()
                                            .setUid(UUID.randomUUID().toString())
                                        .build();
        this.expirationConfig = ExpirationConfiguration
                                    .builder()
                                        .setEternal(true)
                                    .build();

        this.listener = mock(CacheEntryEventListener.class);
    }

    @Test
    public void testBuild() {
        final String cacheName = "test";
        final var config1 = CacheConfiguration
                                .builder()
                                    .setCacheName(cacheName)
                                    .setMemoryStoreConfiguration(memoryStoreConfig)
                                    .setPersistentStoreConfiguration(persistentStoreConfig)
                                    .setEvictionPolicy(evictionPolicy)
                                    .setExpirationConfiguration(expirationConfig)
                                    .addCacheEntryEventListener(listener)
                                .build();

        assertEquals(cacheName, config1.cacheName(), "Cache name must be equal to the value set in builder");
        assertEquals(memoryStoreConfig, config1.memoryStoreConfiguration(), "Memory store config must be equal to config to the value set in builder");
        assertTrue(config1.persistentStoreConfiguration().filter(persistentStoreConfig::equals).isPresent(), "Persistent store config must be equal to config to the value set in builder");
        assertEquals(expirationConfig, config1.expirationConfiguration(), "Expiration config must be equal to config to the value set in builder");
        assertEquals(evictionPolicy, config1.evictionPolicy(), "Eviction policy must be equal to policy to the value set in builder");
        assertEquals(1, config1.eventListeners().size(), "Listeners count must be equal to 1");
        assertEquals(listener, config1.eventListeners().get(0), "Listener must be equal to listener to the value set in builder");

        final var config2 = CacheConfiguration
                                .builder()
                                    .setCacheName(cacheName)
                                    .setMemoryStoreConfiguration(memoryStoreConfig)
                                    .setEvictionPolicy(evictionPolicy)
                                    .setExpirationConfiguration(expirationConfig)
                                .build();

        assertTrue(config2.persistentStoreConfiguration().isEmpty(), "Persistent store config must be empty");
        assertTrue(config2.eventListeners().isEmpty(), "Event listeners must be empty");
    }

    @Test
    public void testBuilderWithEmptyCacheName() {
        final var builder = CacheConfiguration
                                .builder()
                                    .setCacheName(" ")
                                    .setMemoryStoreConfiguration(memoryStoreConfig)
                                    .setEvictionPolicy(evictionPolicy)
                                    .setExpirationConfiguration(expirationConfig)
                                    .addCacheEntryEventListener(listener);

        assertThrows(InvalidConfigurationException.class, builder::build, "Exception must be thrown when cache name is blank string");
    }

    @Test
    public void testBuilderWithNullMemoryStoreConfig() {
        final var builder = CacheConfiguration
                                .builder()
                                    .setCacheName("test")
                                    .setEvictionPolicy(evictionPolicy)
                                    .setExpirationConfiguration(expirationConfig);

        assertThrows(InvalidConfigurationException.class, builder::build, "Memory store config must be set");
    }

    @Test
    void testBuilderWithNullExpirationConfig() {
        final var builder = CacheConfiguration
                                .builder()
                                    .setCacheName("test")
                                    .setEvictionPolicy(CacheConfiguration.EvictionPolicy.LRU)
                                    .setMemoryStoreConfiguration(memoryStoreConfig);

        assertThrows(InvalidConfigurationException.class, builder::build, "Expiration config must be set");
    }
}
