package ru.joke.memcache.clustering.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.joke.cache.bus.core.Cache;
import ru.joke.cache.bus.core.impl.ImmutableComponentState;
import ru.joke.cache.bus.core.state.ComponentState;
import ru.joke.memcache.core.Lifecycle;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.MemCacheManager;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemCacheManagerBusAdapterTest {

    private MemCacheManager memCacheManager;
    private MemCacheManagerBusAdapter cacheManagerAdapter;

    @BeforeEach
    void setUp() {
        this.memCacheManager = mock(MemCacheManager.class);
        this.cacheManagerAdapter = new MemCacheManagerBusAdapter(memCacheManager);
    }

    @Test
    public void testGetUnderlyingCacheManager() {
        final MemCacheManager result = cacheManagerAdapter.getUnderlyingCacheManager(MemCacheManager.class);
        assertEquals(memCacheManager, result, "Underlying cache manager should be returned");
    }

    @Test
    public void testGetUnderlyingCacheManagerShouldThrowException() {
        assertThrows(ClassCastException.class, () -> cacheManagerAdapter.getUnderlyingCacheManager(Integer.class), "Exception must be thrown on incorrect cache manager type");
    }

    @Test
    public void testGetCache() {
        final String cacheName = "cache";
        final var memCache = mock(MemCache.class);
        when(memCacheManager.getCache(cacheName)).thenReturn(Optional.of(memCache));

        final var result = cacheManagerAdapter.getCache(cacheName).orElse(null);

        assertNotNull(result, "Cache must be not null");
        assertTrue(result instanceof MemCacheBusAdapter<Serializable, Serializable>, "Cache must be instanceof ");
    }

    @Test
    public void testGetNonExistingCache() {
        final String cacheName = "non-existing-cache";
        when(memCacheManager.getCache(cacheName)).thenReturn(Optional.empty());

        final Optional<Cache<String, String>> result = cacheManagerAdapter.getCache(cacheName);
        assertTrue(result.isEmpty(), "Cache must not present");
    }

    @Test
    public void testNotReadyComponentState() {
        final ComponentState expectedState = new ImmutableComponentState("memcache-manager", ComponentState.Status.UP_NOT_READY);
        when(memCacheManager.status()).thenReturn(Lifecycle.ComponentStatus.INITIALIZING);

        final ComponentState result = cacheManagerAdapter.state();
        assertEquals(expectedState, result, "State must be equal");
    }

    @Test
    public void testDownComponentState() {
        final ComponentState expectedState = new ImmutableComponentState("memcache-manager", ComponentState.Status.DOWN);

        for (Lifecycle.ComponentStatus status : List.of(Lifecycle.ComponentStatus.TERMINATED, Lifecycle.ComponentStatus.STOPPING, Lifecycle.ComponentStatus.UNAVAILABLE)) {
            when(memCacheManager.status()).thenReturn(status);

            final ComponentState result = cacheManagerAdapter.state();
            assertEquals(expectedState, result, "State must be equal");
        }
    }

    @Test
    public void testFailedComponentState() {
        final ComponentState expectedState = new ImmutableComponentState("memcache-manager", ComponentState.Status.UP_FATAL_BROKEN);
        when(memCacheManager.status()).thenReturn(Lifecycle.ComponentStatus.FAILED);

        final ComponentState result = cacheManagerAdapter.state();
        assertEquals(expectedState, result, "State must be equal");
    }
}
