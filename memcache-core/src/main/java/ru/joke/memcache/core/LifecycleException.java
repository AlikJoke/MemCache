package ru.joke.memcache.core;

import javax.annotation.Nonnull;

/**
 * An exception indicating that an exceptional situation has occurred in a certain operation,
 * related to an invalid component lifecycle state.
 *
 * @author Alik
 * @see MemCacheException
 */
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
