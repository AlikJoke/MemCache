package ru.joke.memcache.core.internal;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

abstract class PersistentCacheRepository {

    @Nonnull
    abstract <K extends Serializable, V extends Serializable> Collection<MemCacheEntry<K, V>> load();

    abstract <K extends Serializable, V extends Serializable> void save(@Nonnull Collection<MemCacheEntry<K, V>> entries);

    static class NoPersistentCacheRepository extends PersistentCacheRepository {

        @Nonnull
        @Override
        public <K extends Serializable, V extends Serializable> Collection<MemCacheEntry<K, V>> load() {
            return Collections.emptySet();
        }

        @Override
        public <K extends Serializable, V extends Serializable> void save(@Nonnull Collection<MemCacheEntry<K, V>> entries) {
        }
    }
}
