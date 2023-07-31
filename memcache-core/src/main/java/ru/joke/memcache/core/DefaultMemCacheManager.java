package ru.joke.memcache.core;

import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.Configuration;
import ru.joke.memcache.core.configuration.ConfigurationSource;
import ru.joke.memcache.core.internal.InternalMemCacheManager;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Closeable;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

@ThreadSafe
public final class DefaultMemCacheManager implements MemCacheManager, Closeable {

    private final MemCacheManager delegateManager;

    public DefaultMemCacheManager() {
        this.delegateManager = new InternalMemCacheManager();
    }

    public DefaultMemCacheManager(@Nonnull ConfigurationSource configurationSource) {
        this.delegateManager = new InternalMemCacheManager(configurationSource);
    }

    public DefaultMemCacheManager(@Nonnull Configuration configuration) {
        this.delegateManager = new InternalMemCacheManager(configuration);
    }

    @Override
    public void initialize() {
        this.delegateManager.initialize();
    }

    @Override
    public boolean createCache(@Nonnull CacheConfiguration configuration) {
        return this.delegateManager.createCache(configuration);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public <K extends Serializable, V extends Serializable> Optional<MemCache<K, V>> getCache(@Nonnull String cacheName) {
        return this.delegateManager.getCache(cacheName);
    }

    @Override
    @Nonnull
    public Set<String> getCacheNames() {
        return this.delegateManager.getCacheNames();
    }

    @Nonnull
    @Override
    public ComponentStatus status() {
        return this.delegateManager.status();
    }

    @Override
    public void shutdown() {
        this.delegateManager.shutdown();
    }

    @Override
    public void close() {
        shutdown();
    }
}
