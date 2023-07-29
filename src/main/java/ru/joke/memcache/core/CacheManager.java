package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.ConfigurationSource;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public interface CacheManager {

    void init();

    void init(@Nonnull ConfigurationSource configurationSource);

    void createCache(@Nonnull CacheConfiguration configuration);

    @Nonnull
    @CheckReturnValue
    <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName);

    @Nonnull
    Set<String> getCacheNames();
}
