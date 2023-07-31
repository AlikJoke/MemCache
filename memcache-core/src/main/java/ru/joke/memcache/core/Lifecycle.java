package ru.joke.memcache.core;

import javax.annotation.Nonnull;

public interface Lifecycle {

    void initialize();

    void shutdown();

    @Nonnull
    ComponentStatus status();

    enum ComponentStatus {

        UNAVAILABLE,

        INITIALIZING,

        RUNNING,

        STOPPING,

        TERMINATED,

        FAILED
    }
}
