package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public interface MemCacheManager extends Lifecycle {

    boolean createCache(@Nonnull CacheConfiguration configuration);

    boolean removeCache(@Nonnull String cacheName);

    @Nonnull
    @CheckReturnValue
    <K extends Serializable, V extends Serializable> Optional<MemCache<K, V>> getCache(@Nonnull String cacheName);

    @Nonnull
    Set<String> getCacheNames();
}
