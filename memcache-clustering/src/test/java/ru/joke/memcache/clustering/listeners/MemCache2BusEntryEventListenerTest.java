package ru.joke.memcache.clustering.listeners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEntryEventType;
import ru.joke.memcache.core.events.CacheEntriesEvent;
import ru.joke.memcache.core.events.CacheEntryEvent;
import ru.joke.memcache.core.events.EventType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class MemCache2BusEntryEventListenerTest {

    private CacheBus cacheBus;
    private MemCache2BusEntryEventListener<String, Integer> eventListener;
    private ArgumentCaptor< ru.joke.cache.bus.core.CacheEntryEvent<?, ?>> eventCaptor;

    @BeforeEach
    void setUp() {
        cacheBus = mock(CacheBus.class);
        eventCaptor = ArgumentCaptor.forClass(ru.joke.cache.bus.core.CacheEntryEvent.class);
        doNothing().when(cacheBus).send(this.eventCaptor.capture());

        eventListener = new MemCache2BusEntryEventListener<>("listener1", cacheBus, "cache");
    }

    @Test
    public void testOnEvent() {
        CacheEntryEvent<String, Integer> cacheEvent = mock(CacheEntryEvent.class);
        when(cacheEvent.key()).thenReturn("key");
        when(cacheEvent.oldValue()).thenReturn(Optional.of(1));
        when(cacheEvent.newValue()).thenReturn(Optional.of(2));
        when(cacheEvent.eventType()).thenReturn(EventType.UPDATED);

        eventListener.onEvent(cacheEvent);

        assertNotNull(this.eventCaptor.getValue(), "Event should be sent to bus");
        assertEquals("key", this.eventCaptor.getValue().key(), "Key must be equal");
        assertEquals(1, this.eventCaptor.getValue().oldValue(), "Old value must be equal");
        assertEquals(2, this.eventCaptor.getValue().newValue(), "New value must be equal");
        assertEquals(CacheEntryEventType.UPDATED, this.eventCaptor.getValue().eventType(), "Event type must be equal");
        assertEquals("cache", this.eventCaptor.getValue().cacheName(), "Cache name must be equal");
    }

    @Test
    public void testOnBatchEvent() {
        CacheEntriesEvent<String, Integer> cacheEntriesEvent = mock(CacheEntriesEvent.class);
        when(cacheEntriesEvent.eventType()).thenReturn(EventType.REMOVED);

        eventListener.onBatchEvent(cacheEntriesEvent);

        assertNotNull(this.eventCaptor.getValue(), "Batch type should be sent to bus");
        assertEquals(CacheEntryEventType.EVICTED, this.eventCaptor.getValue().eventType(), "Event type must be equal");
    }
}
