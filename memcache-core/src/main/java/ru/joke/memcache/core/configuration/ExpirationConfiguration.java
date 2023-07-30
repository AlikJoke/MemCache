package ru.joke.memcache.core.configuration;

public interface ExpirationConfiguration {

    long idleExpirationTimeout();

    long lifespan();

    default boolean eternal() {
        return idleExpirationTimeout() == -1 && lifespan() == -1;
    }
}
