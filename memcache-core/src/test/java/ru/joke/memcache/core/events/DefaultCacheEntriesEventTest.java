package ru.joke.memcache.core.events;

import org.junit.jupiter.api.Test;
import ru.joke.memcache.core.MemCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class DefaultCacheEntriesEventTest {

    @Test
    public void testConstructorShouldSetEventTypeAndSource() {
        final EventType eventType = EventType.UPDATED;

        final MemCache<?, ?> source = mock(MemCache.class);
        final var event = new DefaultCacheEntriesEvent<>(eventType, source);

        assertEquals(eventType, event.eventType(), "Event type must be equal to the value set");
        assertEquals(source, event.source(), "Source cache must be equal to the value set");
    }

    @Test
    public void testConstructorShouldThrowExceptionIfEventTypeIsNull() {
        final MemCache<?, ?> source = mock(MemCache.class);

        assertThrows(
                NullPointerException.class,
                () -> new DefaultCacheEntriesEvent<>(null, source),
                "Exception must be thrown when event type is null"
        );
    }

    @Test
    public void testConstructorShouldThrowExceptionIfSourceIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> new DefaultCacheEntriesEvent<>(EventType.REMOVED, null),
                "Exception must be thrown when event type is null"
        );
    }
}
