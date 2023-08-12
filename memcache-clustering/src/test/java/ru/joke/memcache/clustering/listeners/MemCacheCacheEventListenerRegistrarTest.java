package ru.joke.memcache.clustering.listeners;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.joke.cache.bus.core.CacheBus;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.memcache.clustering.adapters.MemCacheBusAdapter;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemCacheCacheEventListenerRegistrarTest {

    private MemCache<String, String> cache;
    private CacheBus cacheBus;
    private ArgumentCaptor<CacheEventListener<String, String>> listenerCaptor;

    @BeforeEach
    void setUp() {
        this.cacheBus = mock(CacheBus.class);
        this.listenerCaptor = ArgumentCaptor.forClass(CacheEventListener.class);
        this.cache = mock(MemCache.class);
        when(this.cache.name()).thenReturn("test");
        when(this.cache.registerEventListener((CacheEntryEventListener<String, String>) this.listenerCaptor.capture())).thenReturn(true);
        lenient().when(this.cache.deregisterEventListener((CacheEntryEventListener<String, String>) this.listenerCaptor.capture())).thenReturn(true);
    }

    @Test
    public void testRegistration() {

        final MemCacheCacheEventListenerRegistrar registrar = new MemCacheCacheEventListenerRegistrar();
        final MemCacheBusAdapter<String, String> cacheAdapter = new MemCacheBusAdapter<>(cache);

        registrar.registerFor(cacheBus, cacheAdapter);

        assertNotNull(listenerCaptor.getValue(), "Listener must be not null");
    }

    @Test
    public void testRemoveRegistration() {
        final MemCacheCacheEventListenerRegistrar registrar = new MemCacheCacheEventListenerRegistrar();
        final MemCacheBusAdapter<String, String> cacheAdapter = new MemCacheBusAdapter<>(cache);

        registrar.registerFor(this.cacheBus, cacheAdapter);
        registrar.unregisterFor(this.cacheBus, cacheAdapter);

        assertEquals(2, listenerCaptor.getAllValues().size(), "Must be captured 2 listeners");
        assertEquals(listenerCaptor.getAllValues().get(0), listenerCaptor.getAllValues().get(1), "Registered listener and unregistered must be equal");
    }
}
