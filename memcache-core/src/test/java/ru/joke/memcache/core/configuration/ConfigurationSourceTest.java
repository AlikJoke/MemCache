package ru.joke.memcache.core.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ConfigurationSourceTest {

    private ConfigurationSource.SimpleConfigurationSource configurationSource;

    @BeforeEach
    public void setUp() {
        this.configurationSource = ConfigurationSource.createDefault();
    }

    @Test
    public void testPullDefaultConfiguration() {
        final var configuration = configurationSource.pull();
        assertNotNull(configuration, "Configuration must be not null");
        assertEquals(1, configuration.cleaningPoolSize(), "Default cleaning pool size must be set");
        final int parallelismLevel = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        assertEquals(parallelismLevel, configuration.asyncCacheOpsParallelismLevel(), "Default parallelism level must be set");
    }

    @Test
    public void testAdd() {
        final var cacheConfiguration = mock(CacheConfiguration.class);
        configurationSource.add(cacheConfiguration);

        final var cacheConfigurations = configurationSource.pull().cacheConfigurations();

        assertEquals(1, cacheConfigurations.size(), "Configurations must contain only one cache config");
        assertTrue(cacheConfigurations.contains(cacheConfiguration), "Added cache configuration must be in result configs list");
    }

    @Test
    public void testAddAll() {
        final Set<CacheConfiguration> cacheConfigurations = new HashSet<>();
        cacheConfigurations.add(mock(CacheConfiguration.class));
        cacheConfigurations.add(mock(CacheConfiguration.class));

        configurationSource.addAll(cacheConfigurations);

        final var pulledCacheConfigurations = configurationSource.pull().cacheConfigurations();

        assertEquals(cacheConfigurations, pulledCacheConfigurations, "Cache configurations list must be equal");
    }

    @Test
    public void testClear() {
        configurationSource.add(mock(CacheConfiguration.class));
        configurationSource.clear();

        final var cacheConfigurations = configurationSource.pull().cacheConfigurations();

        assertTrue(cacheConfigurations.isEmpty(), "Configurations list must be empty after cleaning of source");
    }

    @Test
    public void testSetCleaningPoolSize() {
        final int cleaningPoolSize = 2;
        configurationSource.setCleaningPoolSize(cleaningPoolSize);

        final int pulledCleaningPoolSize = configurationSource.pull().cleaningPoolSize();

        assertEquals(cleaningPoolSize, pulledCleaningPoolSize, "Cleaning pool size must be equal to the value set");
    }

    @Test
    public void testSetAsyncCacheOpsParallelismLevel() {
        final int parallelismLevel = 4;
        configurationSource.setAsyncCacheOpsParallelismLevel(parallelismLevel);

        final int pulledParallelismLevel = configurationSource.pull().asyncCacheOpsParallelismLevel();

        assertEquals(parallelismLevel, pulledParallelismLevel, "Async ops parallelism level must be equal to value set");
    }

    @Test
    public void testSetIllegalAsyncCacheOpsParallelismLevel() {
        configurationSource.setAsyncCacheOpsParallelismLevel(0);
        assertThrows(InvalidConfigurationException.class, configurationSource::pull, "Exception must be thrown when invalid async ops parallelism level is set");
    }

    @Test
    public void testSetIllegalCleaningPoolSize() {
        configurationSource.setCleaningPoolSize(0);

        assertThrows(InvalidConfigurationException.class, configurationSource::pull, "Exception must be thrown when invalid cleaning pool size is set");
    }
}
