# MemCache
A caching library that uses the local Java process heap to store data. 
The cache is local, but can be clustered when using the CacheBus cache clustering library (https://github.com/AlikJoke/CacheBus).
The cache supports gathering statistics during its operation.

## MemCache settings
The cache supports the following settings: 
1. Eviction policy (LFU/LRU/MRU/FIFO/LIFO)
2. Expiration of elements based on storage settings
3. Settings for the number of elements stored in the cache
4. Event listeners for the cache
5. Ability to save cache elements to a persistent storage for recovery of cache contents when the application restarts

Caching settings can be configured using either XML files or Java API.

## MemCache API
The basic abstractions of the library are ```ru.joke.memcache.core.MemCacheManager``` and ```ru.joke.memcache.core.MemCache```. 
To start using the library, it is necessary to create a MemCache manager object (```ru.joke.memcache.core.DefaultMemCacheManager```), 
which can be passed either a caching configuration (```ru.joke.memcache.core.configuration.Configuration```) or a configuration source 
that provides the configuration in a simpler way (```ru.joke.memcache.core.configuration.ConfigurationSource```). 
The configuration source can be either an XML file-based configuration source (```ru.joke.memcache.core.configuration.XmlConfigurationSource```) 
or a manually created configuration source using the Java API (```ru.joke.memcache.core.configuration.ConfigurationSource.SimpleConfigurationSource```). 
After creating and initializing the cache manager (```ru.joke.memcache.core.DefaultMemCacheManager#initialize()```), it is possible to retrieve and 
work with configured caches created from the provided configuration. After finishing work with the caches (e.g. when the application is shutting down), 
it is necessary to call the cache manager's shutdown method (```ru.joke.memcache.core.DefaultMemCacheManager#shutdown()```) for proper termination of the MemCache.

## Benchmarks
Here are the results of running benchmarks on a cache with a size of 100,000 elements in single-threaded mode 
and with the number of threads corresponding to the set concurrency level for the cache. 
The tests were conducted in an environment with the following characteristics: 
1. Processor: Intel(R) Core(TM) i7-10510U CPU @ 1.80GHz   2.30 GHz
2. VM version: JDK 19.0.1, Java HotSpot(TM) 64-Bit Server VM, 19.0.1+10-21
3. Java Heap (Xmx=Xms): 4G
4. Garbage Collector: G1

The tests were performed for the following operations: 
1. Getting a value from the cache by key (```get```)
2. Adding to an empty cache (```putToEmptyCache```)
3. Adding to a full cache (to account for overhead costs of evicting elements from the cache) (```putToFullCache```)
4. Removing from the cache by key (```remove```)
5. Replacing an element in the cache by key and value (```replace```)

The single-threaded tests refer to ```MemCacheSingleThreadBenchmark```, 
the multithreaded tests refer to ```MemCacheMultipleThreadsBenchmark```.

### Multithreaded tests results
```
Benchmark                                                   Mode       Cnt       Score    Error   Units
MemCacheMultipleThreadsBenchmark.get                       thrpt        10     124,250 ? 15,322  ops/us
MemCacheMultipleThreadsBenchmark.putToEmptyCache           thrpt        10       2,412 ?  0,048  ops/us
MemCacheMultipleThreadsBenchmark.putToFullCache            thrpt        10       2,421 ?  0,096  ops/us
MemCacheMultipleThreadsBenchmark.remove                    thrpt        10      39,664 ?  3,098  ops/us
MemCacheMultipleThreadsBenchmark.replace                   thrpt        10      17,470 ?  0,890  ops/us
MemCacheMultipleThreadsBenchmark.get                        avgt        10       0,028 ?  0,002   us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache            avgt        10       1,651 ?  0,050   us/op
MemCacheMultipleThreadsBenchmark.putToFullCache             avgt        10       1,624 ?  0,043   us/op
MemCacheMultipleThreadsBenchmark.remove                     avgt        10       0,095 ?  0,005   us/op
MemCacheMultipleThreadsBenchmark.replace                    avgt        10       0,225 ?  0,014   us/op
MemCacheMultipleThreadsBenchmark.get                      sample   9197160       0,081 ?  0,004   us/op
MemCacheMultipleThreadsBenchmark.get:p0.50                sample                 0,100            us/op
MemCacheMultipleThreadsBenchmark.get:p0.90                sample                 0,100            us/op
MemCacheMultipleThreadsBenchmark.get:p0.95                sample                 0,200            us/op
MemCacheMultipleThreadsBenchmark.get:p0.99                sample                 0,200            us/op
MemCacheMultipleThreadsBenchmark.get:p0.999               sample                 0,600            us/op
MemCacheMultipleThreadsBenchmark.get:p0.9999              sample                12,923            us/op
MemCacheMultipleThreadsBenchmark.get:p1.00                sample             10977,280            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache          sample  14734859       1,811 ?  0,031   us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p0.50    sample                 0,400            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p0.90    sample                 2,000            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p0.95    sample                 3,000            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p0.99    sample                31,200            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p0.999   sample               121,088            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p0.9999  sample               242,432            us/op
MemCacheMultipleThreadsBenchmark.putToEmptyCache:p1.00    sample             45547,520            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache           sample  14391508       1,870 ?  0,035   us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p0.50     sample                 0,400            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p0.90     sample                 2,000            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p0.95     sample                 3,100            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p0.99     sample                32,288            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p0.999    sample               126,336            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p0.9999   sample               252,633            us/op
MemCacheMultipleThreadsBenchmark.putToFullCache:p1.00     sample             39321,600            us/op
MemCacheMultipleThreadsBenchmark.remove                   sample  14940282       0,149 ?  0,008   us/op
MemCacheMultipleThreadsBenchmark.remove:p0.50             sample                 0,100            us/op
MemCacheMultipleThreadsBenchmark.remove:p0.90             sample                 0,200            us/op
MemCacheMultipleThreadsBenchmark.remove:p0.95             sample                 0,200            us/op
MemCacheMultipleThreadsBenchmark.remove:p0.99             sample                 0,400            us/op
MemCacheMultipleThreadsBenchmark.remove:p0.999            sample                 1,000            us/op
MemCacheMultipleThreadsBenchmark.remove:p0.9999           sample                21,789            us/op
MemCacheMultipleThreadsBenchmark.remove:p1.00             sample             15138,816            us/op
MemCacheMultipleThreadsBenchmark.replace                  sample  13746008       0,268 ?  0,009   us/op
MemCacheMultipleThreadsBenchmark.replace:p0.50            sample                 0,200            us/op
MemCacheMultipleThreadsBenchmark.replace:p0.90            sample                 0,300            us/op
MemCacheMultipleThreadsBenchmark.replace:p0.95            sample                 0,400            us/op
MemCacheMultipleThreadsBenchmark.replace:p0.99            sample                 0,600            us/op
MemCacheMultipleThreadsBenchmark.replace:p0.999           sample                 2,000            us/op
MemCacheMultipleThreadsBenchmark.replace:p0.9999          sample                22,496            us/op
MemCacheMultipleThreadsBenchmark.replace:p1.00            sample             15597,568            us/op
```
### Single-threaded tests results
```
Benchmark                                                   Mode       Cnt       Score    Error   Units
MemCacheSingleThreadBenchmark.get                          thrpt        10      45,938 ?  9,308  ops/us
MemCacheSingleThreadBenchmark.putToEmptyCache              thrpt        10       1,390 ?  0,417  ops/us
MemCacheSingleThreadBenchmark.putToFullCache               thrpt        10       1,594 ?  0,050  ops/us
MemCacheSingleThreadBenchmark.remove                       thrpt        10      20,086 ?  3,112  ops/us
MemCacheSingleThreadBenchmark.replace                      thrpt        10       8,642 ?  0,773  ops/us
MemCacheSingleThreadBenchmark.get                           avgt        10       0,023 ?  0,005   us/op
MemCacheSingleThreadBenchmark.putToEmptyCache               avgt        10       0,609 ?  0,014   us/op
MemCacheSingleThreadBenchmark.putToFullCache                avgt        10       0,631 ?  0,023   us/op
MemCacheSingleThreadBenchmark.remove                        avgt        10       0,047 ?  0,008   us/op
MemCacheSingleThreadBenchmark.replace                       avgt        10       0,117 ?  0,010   us/op
MemCacheSingleThreadBenchmark.get                         sample   2749468       0,082 ?  0,001   us/op
MemCacheSingleThreadBenchmark.get:p0.50                   sample                 0,100            us/op
MemCacheSingleThreadBenchmark.get:p0.90                   sample                 0,100            us/op
MemCacheSingleThreadBenchmark.get:p0.95                   sample                 0,200            us/op
MemCacheSingleThreadBenchmark.get:p0.99                   sample                 0,400            us/op
MemCacheSingleThreadBenchmark.get:p0.999                  sample                 0,600            us/op
MemCacheSingleThreadBenchmark.get:p0.9999                 sample                13,792            us/op
MemCacheSingleThreadBenchmark.get:p1.00                   sample               180,480            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache             sample   2671070       0,866 ?  0,281   us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p0.50       sample                 0,600            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p0.90       sample                 1,000            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p0.95       sample                 1,100            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p0.99       sample                 1,600            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p0.999      sample                 6,296            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p0.9999     sample                32,384            us/op
MemCacheSingleThreadBenchmark.putToEmptyCache:p1.00       sample            213123,072            us/op
MemCacheSingleThreadBenchmark.putToFullCache              sample   2371577       0,727 ?  0,022   us/op
MemCacheSingleThreadBenchmark.putToFullCache:p0.50        sample                 0,600            us/op
MemCacheSingleThreadBenchmark.putToFullCache:p0.90        sample                 0,900            us/op
MemCacheSingleThreadBenchmark.putToFullCache:p0.95        sample                 1,100            us/op
MemCacheSingleThreadBenchmark.putToFullCache:p0.99        sample                 1,500            us/op
MemCacheSingleThreadBenchmark.putToFullCache:p0.999       sample                 5,896            us/op
MemCacheSingleThreadBenchmark.putToFullCache:p0.9999      sample                29,361            us/op
MemCacheSingleThreadBenchmark.putToFullCache:p1.00        sample              9142,272            us/op
MemCacheSingleThreadBenchmark.remove                      sample   3285625       0,090 ?  0,014   us/op
MemCacheSingleThreadBenchmark.remove:p0.50                sample                 0,100            us/op
MemCacheSingleThreadBenchmark.remove:p0.90                sample                 0,100            us/op
MemCacheSingleThreadBenchmark.remove:p0.95                sample                 0,200            us/op
MemCacheSingleThreadBenchmark.remove:p0.99                sample                 0,300            us/op
MemCacheSingleThreadBenchmark.remove:p0.999               sample                 0,600            us/op
MemCacheSingleThreadBenchmark.remove:p0.9999              sample                12,896            us/op
MemCacheSingleThreadBenchmark.remove:p1.00                sample             13729,792            us/op
MemCacheSingleThreadBenchmark.replace                     sample   3191889       0,146 ?  0,001   us/op
MemCacheSingleThreadBenchmark.replace:p0.50               sample                 0,100            us/op
MemCacheSingleThreadBenchmark.replace:p0.90               sample                 0,200            us/op
MemCacheSingleThreadBenchmark.replace:p0.95               sample                 0,200            us/op
MemCacheSingleThreadBenchmark.replace:p0.99               sample                 0,500            us/op
MemCacheSingleThreadBenchmark.replace:p0.999              sample                 1,100            us/op
MemCacheSingleThreadBenchmark.replace:p0.9999             sample                14,800            us/op
MemCacheSingleThreadBenchmark.replace:p1.00               sample               229,376            us/op
```
