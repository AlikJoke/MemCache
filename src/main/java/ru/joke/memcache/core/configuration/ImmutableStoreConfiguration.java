package ru.joke.memcache.core.configuration;

public record ImmutableStoreConfiguration(
        int concurrencyLevel,
        long maxElements
) implements StoreConfiguration {
}
