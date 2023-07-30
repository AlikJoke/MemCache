package ru.joke.memcache.core.configuration;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Immutable
public record ImmutableExpirationConfiguration(
        long idleExpirationTimeout,
        long lifespan
) implements ExpirationConfiguration {

    public ImmutableExpirationConfiguration {
        if (idleExpirationTimeout < -1 || lifespan < -1) {
            throw new InvalidConfigurationException("Expiration timeout must be non negative");
        }
    }
}
