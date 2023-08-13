package ru.joke.memcache.core.events;

/**
 * Type of cache element change event.
 *
 * @author Alik
 */
public enum EventType {

    /**
     * Added a new element
     */
    ADDED,

    /**
     * Modified an existing element in the cache
     */
    UPDATED,

    /**
     * Removed an element from the cache
     */
    REMOVED,

    /**
     * Removed an expired element from the cache
     */
    EXPIRED
}
