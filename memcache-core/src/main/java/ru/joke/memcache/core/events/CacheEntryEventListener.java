package ru.joke.memcache.core.events;

import javax.annotation.Nonnull;
import java.io.Serializable;

public interface CacheEntryEventListener<K extends Serializable, V extends Serializable> {

    void onEvent(@Nonnull CacheEntryEvent<? extends K, ? extends V> event);

    void onBatchEvent(@Nonnull CacheEntriesEvent<? extends K, ? extends V> event);
}
