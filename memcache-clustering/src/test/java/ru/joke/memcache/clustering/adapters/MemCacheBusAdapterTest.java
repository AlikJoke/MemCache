package ru.joke.memcache.clustering.adapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.joke.cache.bus.core.CacheEventListener;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.events.CacheEntriesEvent;
import ru.joke.memcache.core.events.CacheEntryEvent;
import ru.joke.memcache.core.events.CacheEntryEventListener;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class MemCacheBusAdapterTest {

    private MemCache<String, String> mockMemCache;
    private MemCacheBusAdapter<String, String> memCacheBusAdapter;

    @BeforeEach
    void setUp() {
        this.mockMemCache = mock(MemCache.class);
        this.memCacheBusAdapter = new MemCacheBusAdapter<>(mockMemCache);
    }

    @Test
    public void testGetCacheName() {
        final String name = "memCache";
        when(mockMemCache.name()).thenReturn(name);

        final String result = memCacheBusAdapter.getName();
        assertEquals(name, result, "Cache name must be equal");
    }

    @Test
    public void testGetByKeyFromCache() {
        final String key = "key";
        final String value = "value";
        when(mockMemCache.get(key)).thenReturn(Optional.of(value));

        final Optional<String> result = memCacheBusAdapter.get(key);
        assertEquals(Optional.of(value), result, "Value from cache must be equal");
    }

    @Test
    public void testEvictShouldRemoveKeyFromCache() {
        final String key = "key";
        memCacheBusAdapter.evict(key);
        verify(mockMemCache).remove(key);
    }

    @Test
    public void testRemoveFromCache() {
        final String key = "key";
        final String value = "value";
        when(mockMemCache.remove(key)).thenReturn(Optional.of(value));

        final Optional<String> result = memCacheBusAdapter.remove(key);

        assertEquals(Optional.of(value), result, "Value should be removed");
        verify(mockMemCache).remove(key);
    }

    @Test
    public void testPutInCache() {
        final String key = "key";
        final String value = "value";

        memCacheBusAdapter.put(key, value);
        verify(mockMemCache).put(key, value);
    }

    @Test
    public void testPutIfAbsentInCache() {
        final String key = "key";
        final String value = "value";

        memCacheBusAdapter.putIfAbsent(key, value);
        verify(mockMemCache).putIfAbsent(key, value);
    }

    @Test
    public void testClearCache() {
        memCacheBusAdapter.clear();
        verify(mockMemCache).clear();
    }

    @Test
    public void testMergeOperation() {
        final String key = "key";
        final String newValue = "newValue";

        final BiFunction<String, String, String> mergeFunc = (v1, v2) -> v1 + v2;
        memCacheBusAdapter.merge(key, newValue, mergeFunc);
        verify(mockMemCache).merge(key, newValue, mergeFunc);
    }

    @Test
    public void testComputeIfAbsentOperation() {
        final String key = "key";
        final String value = "value";
        final Function<String, String> func = k -> value;
        when(mockMemCache.computeIfAbsent(key, func)).thenReturn(Optional.of(value));

        final Optional<String> result = memCacheBusAdapter.computeIfAbsent(key, func);
        assertEquals(Optional.of(value), result, "New value must be equal");
    }

    @Test
    public void testRegisterEventListener() {
        final TestListener listener = new TestListener();

        memCacheBusAdapter.registerEventListener(listener);
        verify(mockMemCache).registerEventListener(listener);
    }

    @Test
    public void testRegisterEventListenerThrowCCE() {
        final CacheEventListener<String, String> listener = mock(CacheEventListener.class);
        assertThrows(ClassCastException.class, () -> memCacheBusAdapter.registerEventListener(listener));
    }

    @Test
    public void testUnregisterEventListener() {
        final TestListener listener = new TestListener();
        memCacheBusAdapter.unregisterEventListener(listener);

        verify(mockMemCache).deregisterEventListener(listener);
    }

    @Test
    public void testUnregisterEventListenerThrowCCE() {
        final CacheEventListener<String, String> listener = mock(CacheEventListener.class);
        assertThrows(ClassCastException.class, () -> memCacheBusAdapter.unregisterEventListener(listener));
    }

    private static class TestListener implements CacheEntryEventListener<String, String>, CacheEventListener<String, String> {

        @Override
        public void onEvent(@Nonnull CacheEntryEvent<? extends String, ? extends String> event) {

        }

        @Override
        public void onBatchEvent(@Nonnull CacheEntriesEvent<? extends String, ? extends String> event) {

        }
    }
}
