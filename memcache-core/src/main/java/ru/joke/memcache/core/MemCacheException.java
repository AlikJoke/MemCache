package ru.joke.memcache.core;

import javax.annotation.Nonnull;

public class MemCacheException extends RuntimeException {

    public MemCacheException(@Nonnull String message) {
        super(message);
    }

    public MemCacheException(@Nonnull String message, @Nonnull Exception cause) {
        super(message, cause);
    }

    public MemCacheException(@Nonnull Exception cause) {
        super(cause);
    }
}
