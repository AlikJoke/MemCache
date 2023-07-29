package ru.joke.memcache.core.configuration;

public interface ExpirationConfiguration {

    long idleExpirationTimeout();

    long expirationTimeout();

    default boolean eternal() {
        return idleExpirationTimeout() == -1 && expirationTimeout() == -1;
    }
}
