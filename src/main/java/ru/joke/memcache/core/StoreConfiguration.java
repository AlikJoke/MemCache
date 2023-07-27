package ru.joke.memcache.core;

public interface StoreConfiguration {

    StoreType storeType();

    int concurrencyLevel();

    long maxElementsInHeapMemory();

    int availableOffHeapMemoryBytes();
}
