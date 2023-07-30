package ru.joke.memcache.core.configuration;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Immutable
public record ImmutableStoreConfiguration(
        int concurrencyLevel,
        long maxElements,
        boolean persistOnShutdown
) implements StoreConfiguration {

    public ImmutableStoreConfiguration {
        if (concurrencyLevel < 1) {
            throw new InvalidConfigurationException("Concurrency level must be at least 1");
        } else if (maxElements < 1) {
            throw new InvalidConfigurationException("Max elements count must be at least 1");
        }
    }
}
