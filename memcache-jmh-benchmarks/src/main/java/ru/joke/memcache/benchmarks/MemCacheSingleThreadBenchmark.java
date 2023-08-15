package ru.joke.memcache.benchmarks;

import org.openjdk.jmh.annotations.*;
import ru.joke.memcache.core.MemCache;
import ru.joke.memcache.core.MemCacheManager;
import ru.joke.memcache.core.configuration.CacheConfiguration;
import ru.joke.memcache.core.configuration.ConfigurationSource;
import ru.joke.memcache.core.configuration.ExpirationConfiguration;
import ru.joke.memcache.core.configuration.MemoryStoreConfiguration;
import ru.joke.memcache.core.internal.InternalMemCacheManager;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.SampleTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {"-Xms4G", "-Xmx4G"})
@Warmup(iterations = 2)
@Measurement(iterations = 5)
public class MemCacheSingleThreadBenchmark {

    private MemCacheManager cacheManager;
    private MemCache<Integer, Integer> fullCache;
    private MemCache<Integer, Integer> emptyCache;

    @State(Scope.Thread)
    public static class ThreadKeyState {
        private int startKey = new Random().nextInt(0, 10_000);
    }

    @Setup(Level.Iteration)
    public void setUp() {
        final String fullCacheName = "full";
        final String emptyCacheName = "empty";
        this.cacheManager = new InternalMemCacheManager(buildConfiguration(fullCacheName, emptyCacheName));
        this.cacheManager.initialize();

        this.fullCache = cacheManager.getCache(fullCacheName, Integer.class, Integer.class).orElseThrow();

        for (int i = 0; i < 100_000; i++) {
            this.fullCache.put(i, i);
        }

        this.emptyCache = cacheManager.getCache(emptyCacheName, Integer.class, Integer.class).orElseThrow();
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        this.cacheManager.shutdown();
    }

    @Benchmark
    public Optional<Integer> putToEmptyCache(MemCacheSingleThreadBenchmark.ThreadKeyState state) {
        return this.emptyCache.put(state.startKey, state.startKey++);
    }

    @Benchmark
    public Optional<Integer> putToFullCache(MemCacheSingleThreadBenchmark.ThreadKeyState state) {
        return this.fullCache.put(state.startKey, state.startKey++);
    }

    @Benchmark
    public Optional<Integer> get(MemCacheSingleThreadBenchmark.ThreadKeyState state) {
        return this.fullCache.get(state.startKey++);
    }

    @Benchmark
    public Optional<Integer> remove(MemCacheSingleThreadBenchmark.ThreadKeyState state) {
        return this.fullCache.remove(state.startKey++);
    }

    @Benchmark
    public boolean replace(MemCacheSingleThreadBenchmark.ThreadKeyState state) {
        final Integer key = state.startKey;
        final boolean result = this.fullCache.replace(key, key, 0);
        state.startKey++;

        return result;
    }

    private ConfigurationSource buildConfiguration(final String fullCache, final String emptyCache) {
        return ConfigurationSource
                .createDefault()
                    .setCleaningPoolSize(1)
                    .setAsyncCacheOpsParallelismLevel(1)
                    .add(buildCacheConfiguration(fullCache))
                    .add(buildCacheConfiguration(emptyCache));
    }

    private CacheConfiguration buildCacheConfiguration(final String cacheName) {
        return CacheConfiguration
                .builder()
                    .setCacheName(cacheName)
                    .setMemoryStoreConfiguration(
                            MemoryStoreConfiguration
                                    .builder()
                                        .setMaxEntries(100_000)
                                        .setConcurrencyLevel(4)
                                    .build()
                    )
                    .setExpirationConfiguration(
                            ExpirationConfiguration
                                    .builder()
                                        .setLifespan(60_000)
                                        .setIdleTimeout(30_000)
                                    .build()
                    )
                    .setEvictionPolicy(CacheConfiguration.EvictionPolicy.LRU)
                .build();
    }
}
