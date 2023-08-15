package ru.joke.memcache.benchmarks;

import org.openjdk.jmh.annotations.Threads;

@Threads(4)
public class MemCacheMultipleThreadsBenchmark extends MemCacheSingleThreadBenchmark {
}
