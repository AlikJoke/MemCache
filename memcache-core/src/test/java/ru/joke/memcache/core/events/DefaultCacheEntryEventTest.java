package ru.joke.memcache.core.events;

import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.MemCache;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class DefaultCacheEntryEventTest {

    @Test
    void testConstructorWithNonNullValuesShouldSetKeyOldValueNewValueEventTypeAndSource() {
        Integer key = 1;
        String oldValue = "oldValue";
        String newValue = "newValue";
        EventType eventType = EventType.UPDATED;
        @SuppressWarnings("unchecked")
        MemCache<Integer, String> source = mock(MemCache.class);

        DefaultCacheEntryEvent<Integer, String> event = new DefaultCacheEntryEvent<>(key, oldValue, newValue, eventType, source);

        assertEquals(key, event.key(), "Event key must be equal to the value set");
        assertEquals(Optional.of(oldValue), event.oldValue(), "Old value must be equal to the value set");
        assertEquals(Optional.of(newValue), event.newValue(), "New value must be equal to the value set");
        assertEquals(eventType, event.eventType(), "Event type must be equal to the value set");
        assertEquals(source, event.source(), "Source cache must be equal to the value set");
    }

    @Test
    void testConstructorWithNullOldValueAndNewValueShouldSetKeyEventTypeAndSource() {
        Integer key = 1;
        EventType eventType = EventType.REMOVED;
        @SuppressWarnings("unchecked")
        MemCache<Integer, String> source = mock(MemCache.class);

        final var event = new DefaultCacheEntryEvent<>(key, (String) null, null, eventType, source);

        assertEquals(key, event.key(), "Event key must be equal to the value set");
        assertTrue(event.oldValue().isEmpty(), "Old value must be empty");
        assertTrue(event.newValue().isEmpty(), "New value must be empty");
        assertEquals(eventType, event.eventType(), "Event type must be equal to the value set");
        assertEquals(source, event.source(), "Event source must be equal to the value set");
    }

    @Test
    void testConstructorShouldThrowExceptionIfKeyIsNull() {
        EventType eventType = EventType.UPDATED;
        @SuppressWarnings("unchecked")
        MemCache<Integer, String> source = mock(MemCache.class);

        assertThrows(
                NullPointerException.class,
                () -> new DefaultCacheEntryEvent<>(null, "oldValue", "newValue", eventType, source),
                "Exception must be thrown when key is null"
        );
    }

    @Test
    void testConstructorShouldThrowExceptionIfEventTypeIsNull() {
        @SuppressWarnings("unchecked")
        MemCache<Integer, String> source = mock(MemCache.class);

        assertThrows(
                NullPointerException.class,
                () -> new DefaultCacheEntryEvent<>(1, "oldValue", "newValue", null, source),
                "Exception must be thrown when event type is null"
        );
    }

    @Test
    void testConstructorShouldThrowExceptionIfSourceIsNull() {

        assertThrows(
                NullPointerException.class,
                () -> new DefaultCacheEntryEvent<>(1, "oldValue", "newValue", EventType.UPDATED, null),
                "Exception must be thrown when source is null"
        );
    }
}
