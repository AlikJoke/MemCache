package ru.joke.memcache.core.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpirationConfigurationTest {

    @Test
    public void testEternal() {
        final var config1 = ExpirationConfiguration
                                    .builder()
                                        .setIdleTimeout(-1)
                                        .setLifespan(-1)
                                    .build();
        assertTrue(config1.eternal(), "Cache must be eternal when idle timeout and lifespan equal to -1");

        final var config2 = ExpirationConfiguration
                                    .builder()
                                        .setIdleTimeout(1000)
                                        .setLifespan(2000)
                                    .build();
        assertFalse(config2.eternal(), "Cache must not be eternal when idle timeout and lifespan set to positive value");

        final var config3 = ExpirationConfiguration
                                    .builder()
                                        .setEternal(true)
                                        .setIdleTimeout(1000)
                                        .setLifespan(10000)
                                    .build();
        assertTrue(config3.eternal(), "Cache must be eternal when eternal property is set");
        assertEquals(-1, config3.idleTimeout(), "Idle timeout must be equal to -1 when cache is eternal");
        assertEquals(-1, config3.lifespan(), "Lifespan must be equal to -1 when cache is eternal");
    }

    @Test
    public void testBuilderInvalidTimeout() {
        final var builder1 = ExpirationConfiguration
                                .builder()
                                    .setIdleTimeout(-1000)
                                    .setLifespan(1000);
        assertThrows(InvalidConfigurationException.class, builder1::build, "When idle timeout set to negative value less than -1 then exception should be thrown");

        final var builder2 = ExpirationConfiguration
                                .builder()
                                    .setIdleTimeout(1000)
                                    .setLifespan(-2000);
        assertThrows(InvalidConfigurationException.class, builder2::build, "When lifespan set to negative value less than -1 then exception should be thrown");
    }

    @Test
    public void testBuilderDefaultTimeout() {
        final var config1 = ExpirationConfiguration
                                .builder()
                                    .setEternal(true)
                                .build();
        assertEquals(-1, config1.idleTimeout(), "Idle timeout must be equal to -1 when eternal is true");
        assertEquals(-1, config1.lifespan(), "Lifespan must be equal to -1 when eternal is true");

        final var config2 = ExpirationConfiguration
                                .builder()
                                    .setIdleTimeout(0)
                                    .setLifespan(-1)
                                .build();
        assertEquals(0, config2.idleTimeout(), "Idle timeout must be equal to 0");
        assertEquals(0, config2.lifespan(), "Lifespan must be equal to 0 when lifespan is set to -1 and idle timeout is non-negative");
    }

    @Test
    public void testBuild() {
        final long idleTimeout = 1000;
        final long lifespan = 2000;
        final var config = ExpirationConfiguration
                                .builder()
                                    .setIdleTimeout(idleTimeout)
                                    .setLifespan(lifespan)
                                .build();
        assertEquals(idleTimeout, config.idleTimeout(), "Idle timeout must be equal to value that is set in builder");
        assertEquals(lifespan, config.lifespan(), "Lifespan must be equal to value that is set in builder");
    }

}
