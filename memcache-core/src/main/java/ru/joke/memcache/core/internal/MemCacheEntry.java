package ru.joke.memcache.core.internal;

record MemCacheEntry<K, V>(V value, EntryMetadata<?, K> metadata) {
}