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