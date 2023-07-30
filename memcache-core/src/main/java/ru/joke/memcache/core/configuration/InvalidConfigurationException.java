package ru.joke.memcache.core.configuration;

import javax.annotation.Nonnull;

public class InvalidConfigurationException extends RuntimeException {

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
