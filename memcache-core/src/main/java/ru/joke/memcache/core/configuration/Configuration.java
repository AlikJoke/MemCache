package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.Set;

public interface Configuration {

    @Nonnull
    Set<CacheConfiguration> cacheConfigurations();

    @Nonnegative
    int cleaningPoolSize();

    @Nonnegative
    int asyncCacheOpsParallelismLevel();
}
