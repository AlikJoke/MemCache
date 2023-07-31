package ru.joke.memcache.core;

import javax.annotation.Nonnull;

public class LifecycleException extends MemCacheException {

    public LifecycleException(@Nonnull String message) {
        super(message);
    }

    public LifecycleException(@Nonnull String message, @Nonnull Exception cause) {
        super(message, cause);
    }

    public LifecycleException(@Nonnull Exception cause) {
        super(cause);
    }
}
