package ru.joke.memcache.core.configuration;

import ru.joke.memcache.core.MemCacheException;

import javax.annotation.Nonnull;

/**
 * An exception indicating that the caching configuration has fatal errors, making it
 * impossible for the caching provider to function.
 *
 * @author Alik
 */
public class InvalidConfigurationException extends MemCacheException {

    public InvalidConfigurationException(@Nonnull String message) {
        super(message);
    }

    public InvalidConfigurationException(@Nonnull String message, @Nonnull Exception cause) {
        super(message, cause);
    }

    public InvalidConfigurationException(@Nonnull Exception cause) {
        super(cause);
    }
}
