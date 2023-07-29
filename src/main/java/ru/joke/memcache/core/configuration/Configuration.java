package ru.joke.memcache.core.configuration;

import java.util.Set;

public interface Configuration {

    Set<CacheConfiguration> cacheConfigurations();

    int cleaningPoolSize();
}
