package ru.joke.memcache.core;

public interface EvictionConfiguration {

    EvictionPolicy evictionPolicy();

    long idleExpirationTimeout();

    long expirationTimeout();
}
