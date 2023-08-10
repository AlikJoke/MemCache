package ru.joke.memcache.core.configuration;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PersistentStoreConfigurationTest {

    @Test
    public void testUidWhenLocationIsNotSet() {
        final String uid = UUID.randomUUID().toString();
        final var config = PersistentStoreConfiguration
                                .builder()
                                    .setUid(uid)
                                .build();
        assertEquals(uid, config.uid(), "Uid must be equal value to the value set in builder");
        assertNull(config.location(), "Location must be equal to null");
    }

    @Test
    public void testBuilderInvalidUid() {
        final var config1 = PersistentStoreConfiguration.builder();
        assertThrows(InvalidConfigurationException.class, config1::build, "When uid is not set then exception must be thrown");

        final var config2 = PersistentStoreConfiguration
                                .builder()
                                    .setUid(" ");
        assertThrows(InvalidConfigurationException.class, config2::build, "When uid is set to blank string then exception must be thrown");
    }

    @Test
    public void testBuild() {
        final String uid = UUID.randomUUID().toString();
        final String location = "/path/to/store";
        final var config = PersistentStoreConfiguration
                                .builder()
                                    .setLocation(location)
                                    .setUid(uid)
                                .build();
        assertEquals(location, config.location(), "Location must be equal to the value set in builder");
        assertEquals(uid, config.uid(), "Uid must be equal to the value set in builder");
    }

    @Test
    public void testEqualityOfConfigs() {
        final String uid = UUID.randomUUID().toString();
        final String location = "/path/to/store";
        final var config1 = PersistentStoreConfiguration
                                .builder()
                                    .setLocation(location)
                                    .setUid(uid)
                                .build();
        final var config2 = PersistentStoreConfiguration
                                .builder()
                                    .setLocation(location)
                                    .setUid(uid)
                                .build();
        final var config3 = PersistentStoreConfiguration
                                .builder()
                                    .setLocation(location)
                                    .setUid(uid + "!")
                                .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());

        assertNotEquals(config2, config3);
    }
}
