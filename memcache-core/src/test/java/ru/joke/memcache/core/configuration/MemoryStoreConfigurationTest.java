package ru.joke.memcache.core.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MemoryStoreConfigurationTest {

    @Test
    public void testInvalidConfiguration() {
        final var builder =
                MemoryStoreConfiguration
                    .builder()
                        .setConcurrencyLevel(0)
                        .setMaxEntries(2);

        assertThrows(InvalidConfigurationException.class, builder::build, "Memory store configuration can't contain zero value in concurrency level");

        builder.setConcurrencyLevel(-1);
        assertThrows(InvalidConfigurationException.class, builder::build, "Memory store configuration can't contain negative value in concurrency level");

        builder.setConcurrencyLevel(1);

        builder.setMaxEntries(0);
        assertThrows(InvalidConfigurationException.class, builder::build, "Memory store configuration can't contain zero value in max entries");

        builder.setMaxEntries(-1);
        assertThrows(InvalidConfigurationException.class, builder::build, "Memory store configuration can't contain negative value in max entries");
    }

    @Test
    public void testStdValidConfiguration() {
        final int concurrencyLevel = 2;
        final int maxEntries = 12;

        final var builder =
                MemoryStoreConfiguration
                        .builder()
                        .setConcurrencyLevel(concurrencyLevel)
                        .setMaxEntries(maxEntries);

        final var config = builder.build();
        assertEquals(concurrencyLevel, config.concurrencyLevel(), "Concurrency level must be equal to the value set in builder");
        assertEquals(maxEntries, config.maxEntries(), "Max entries count must be equal to the value set in builder");
    }

    @Test
    public void testValidConfigurationWithConcurrencyLevelOverflow() {
        final int concurrencyLevel = 24;
        final int maxEntries = 12;

        final var builder =
                MemoryStoreConfiguration
                        .builder()
                        .setConcurrencyLevel(concurrencyLevel)
                        .setMaxEntries(maxEntries);

        final var config = builder.build();
        assertEquals(maxEntries, config.concurrencyLevel(), "Concurrency level must be not equal to the value set in builder because that value is higher than max entries count");
        assertEquals(maxEntries, config.maxEntries(), "Max entries count must be equal to the value set in builder");
    }
}
