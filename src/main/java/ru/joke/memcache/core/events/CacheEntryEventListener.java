package ru.joke.memcache.core.events;

import javax.annotation.Nonnull;
import java.io.Serializable;

public interface CacheEntryEventListener<K extends Serializable, V extends Serializable> {

    void onEvent(@Nonnull CacheEntryEvent<K, V> event);

    void onBatchEvent(@Nonnull CacheEntriesEvent<K, V> event);
}
