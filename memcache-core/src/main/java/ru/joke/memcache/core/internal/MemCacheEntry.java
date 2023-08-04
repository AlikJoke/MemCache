package ru.joke.memcache.core.internal;

import javax.annotation.Nonnull;

record MemCacheEntry<K, V>(@Nonnull V value, @Nonnull EntryMetadata<?, K> metadata) {
}