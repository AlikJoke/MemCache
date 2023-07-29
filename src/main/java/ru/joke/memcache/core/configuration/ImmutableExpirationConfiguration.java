package ru.joke.memcache.core.configuration;

public record ImmutableExpirationConfiguration(
        long idleExpirationTimeout,
        long expirationTimeout
) implements ExpirationConfiguration {
}
