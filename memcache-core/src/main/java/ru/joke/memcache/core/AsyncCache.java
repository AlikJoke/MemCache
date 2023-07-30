package ru.joke.memcache.core;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface AsyncCache<K extends Serializable, V extends Serializable> {

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> getAsync(@Nonnull K key);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> removeAsync(@Nonnull K key);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Boolean> removeAsync(@Nonnull K key, @Nonnull V value);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V> >putAsync(@Nonnull K key, @Nullable V value);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> putIfAbsentAsync(@Nonnull K key, @Nullable V value);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Void> clearAsync();

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> mergeAsync(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> mergeFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeIfAbsentAsync(@Nonnull K key, @Nonnull Function<? super K, ? extends V> valueFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Optional<V>> computeIfPresentAsync(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remappingFunction);

    @Nonnull
    @CheckReturnValue
    CompletableFuture<Boolean> replaceAsync(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue);
}
