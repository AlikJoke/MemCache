package ru.joke.memcache.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public interface CacheManager {

    void createCache(@Nonnull CacheConfiguration configuration);

    @Nonnull
    @CheckReturnValue
    <K extends Serializable, V extends Serializable> Optional<Cache<K, V>> getCache(@Nonnull String cacheName);

    @Nonnull
    Set<String> getCacheNames();
}
