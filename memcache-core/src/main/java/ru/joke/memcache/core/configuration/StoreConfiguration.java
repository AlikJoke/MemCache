package ru.joke.memcache.core.configuration;

public interface StoreConfiguration {

    int concurrencyLevel();

    long maxElements();

    boolean persistOnShutdown();
}
