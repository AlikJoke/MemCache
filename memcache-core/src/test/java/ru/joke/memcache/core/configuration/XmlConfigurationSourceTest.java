package ru.joke.memcache.core.configuration;

import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.events.CacheEntriesEvent;
import ru.joke.memcache.core.events.CacheEntryEvent;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class XmlConfigurationSourceTest {

    private static final String INVALID_FILE_NAME = "/test-invalid-configuration.xml";
    private static final String VALID_FILE_NAME1 = "/test-configuration1.xml";
    private static final String VALID_FILE_NAME2 = "/test-configuration2.xml";
    private static final String RELATIVE_PATH = "src/test/resources";

    @Test
    public void testWhenPullFromInvalidXmlExternalFileThenException() {
        final File xmlFile = new File(RELATIVE_PATH + INVALID_FILE_NAME);
        final ConfigurationSource source =
                XmlConfigurationSource
                        .builder()
                            .addExternalConfigurationFile(xmlFile)
                        .build();
        makeChecksInvalid(source);
    }

    @Test
    public void testWhenPullFromInvalidXmlByResourceConfigFilePathThenException() {
        final ConfigurationSource source =
                XmlConfigurationSource
                        .builder()
                            .addResourceConfigurationFilePath(INVALID_FILE_NAME)
                        .build();
        makeChecksInvalid(source);
    }

    @Test
    public void testWhenPullFromValidXmlFileThenOk() {
        final File xmlFile1 = new File(RELATIVE_PATH + VALID_FILE_NAME1);
        final File xmlFile2 = new File(RELATIVE_PATH + VALID_FILE_NAME2);
        final ConfigurationSource source =
                XmlConfigurationSource
                        .builder()
                            .addExternalConfigurationFiles(Set.of(xmlFile1, xmlFile2))
                        .build();
        makeChecksValid(source);
    }

    @Test
    public void testWhenPullFromValidXmlByResourceConfigFilePathThenOk() {
        final ConfigurationSource source =
                XmlConfigurationSource
                        .builder()
                            .addResourceConfigurationFilePaths(Set.of(VALID_FILE_NAME1, VALID_FILE_NAME2))
                        .build();
        makeChecksValid(source);
    }

    private void makeChecksInvalid(final ConfigurationSource source) {
        assertThrows(InvalidConfigurationException.class, source::pull, "Exception must be thrown when the file is invalid");
    }

    private void makeChecksValid(final ConfigurationSource source) {

        final Configuration configuration = source.pull();

        assertEquals(2, configuration.cleaningPoolSize(), "Cleaning pool size must be equal to max value from config files");
        assertEquals(3, configuration.cacheConfigurations().size(), "Configurations count must be equal");
        assertEquals(41, configuration.asyncCacheOpsParallelismLevel(), "Async cache ops parallelism level must be equal to max value from config files");

        final CacheConfiguration cacheConfiguration1 = buildCacheConfig("test1", CacheConfiguration.EvictionPolicy.LFU, 20, 2, "t1", "/opt/loc", true, -1, -1);
        final CacheConfiguration cacheConfiguration2 = buildCacheConfig("test2", CacheConfiguration.EvictionPolicy.FIFO, 30, 3, "t1", "/opt/loc", true, 10000, 100);
        final CacheConfiguration cacheConfiguration3 = buildCacheConfig("test3", CacheConfiguration.EvictionPolicy.LRU, 40, 4, "t2", "/opt/loc2", false, 1000, -1);

        final CacheConfiguration configFromXmlTest1 = findConfigByName(configuration.cacheConfigurations(), "test1");
        final CacheConfiguration configFromXmlTest2 = findConfigByName(configuration.cacheConfigurations(), "test2");
        final CacheConfiguration configFromXmlTest3 = findConfigByName(configuration.cacheConfigurations(), "test3");

        makeCacheConfigChecks(cacheConfiguration1, configFromXmlTest1);
        makeCacheConfigChecks(cacheConfiguration2, configFromXmlTest2);
        makeCacheConfigChecks(cacheConfiguration3, configFromXmlTest3);
    }

    private void makeCacheConfigChecks(final CacheConfiguration xmlConfig, final CacheConfiguration configToCompare) {
        assertEquals(configToCompare.cacheName(), xmlConfig.cacheName(), "Cache name must be equal");
        assertEquals(configToCompare.evictionPolicy(), xmlConfig.evictionPolicy(), "Eviction policy must be equal");
        assertEquals(configToCompare.memoryStoreConfiguration(), xmlConfig.memoryStoreConfiguration(), "Memory store configuration must be equal");
        assertEquals(configToCompare.persistentStoreConfiguration(), xmlConfig.persistentStoreConfiguration(), "Persistent store configuration must be equal");
        assertEquals(configToCompare.expirationConfiguration(), xmlConfig.expirationConfiguration(), "Expiration configuration must be equal");
        assertEquals(configToCompare.eventListeners().size(), xmlConfig.eventListeners().size(), "Event listeners count must be equal");
        assertEquals(configToCompare.eventListeners().get(0).getClass(), xmlConfig.eventListeners().get(0).getClass(), "Event listener must be equal");
    }

    private CacheConfiguration findConfigByName(final Set<CacheConfiguration> configs, final String cacheName) {
        return configs
                .stream()
                .filter(c -> c.cacheName().equals(cacheName))
                .findAny()
                .orElseThrow();
    }

    private CacheConfiguration buildCacheConfig(
            final String cacheName,
            final CacheConfiguration.EvictionPolicy policy,
            final int maxEntries,
            final int concurrencyLevel,
            final String storageUid,
            final String location,
            final boolean eternal,
            final long lifespan,
            final long idleTimeout) {
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
                    .setPersistentStoreConfiguration(
                            PersistentStoreConfiguration
                                    .builder()
                                        .setUid(storageUid)
                                        .setLocation(location)
                                    .build()
                    )
                    .setExpirationConfiguration(
                            ExpirationConfiguration
                                    .builder()
                                        .setEternal(eternal)
                                        .setLifespan(lifespan)
                                        .setIdleTimeout(idleTimeout)
                                    .build()
                    )
                    .setCacheEntryEventListeners(List.of(new Listener1()))
                .build();
    }

    public static class Listener1 implements CacheEntryEventListener<String, String> {

        @Override
        public void onEvent(@Nonnull CacheEntryEvent<? extends String, ? extends String> event) {

        }

        @Override
        public void onBatchEvent(@Nonnull CacheEntriesEvent<? extends String, ? extends String> event) {

        }
    }
}
