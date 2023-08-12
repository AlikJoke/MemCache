package ru.joke.memcache.core;

import javax.annotation.Nonnull;

/**
 * Presentation of the lifecycle of the caching library component.
 *
 * @author Alik
 * @see ComponentStatus
 */
public interface Lifecycle {

    /**
     * Performs component initialization.
     */
    void initialize();

    /**
     * Stops the operation of the component.
     */
    void shutdown();

    /**
     * Returns the lifecycle status of the component.
     *
     * @return status of the component, cannot be {@code null}.
     */
    @Nonnull
    ComponentStatus status();

    /**
     * The lifecycle status of the cache component.
     *
     * @author Alik
     */
    enum ComponentStatus {

        /**
         * Status is unknown or component has not been started yet
         */
        UNAVAILABLE,

        /**
         * Component is in the process of initialization
         */
        INITIALIZING,

        /**
         * Component is active
         */
        RUNNING,

        /**
         * Component is in the process of stopping
         */
        STOPPING,

        /**
         * Component has been stopped
         */
        TERMINATED,

        /**
         * Component is in an error/unrecoverable state
         */
        FAILED
    }
}
