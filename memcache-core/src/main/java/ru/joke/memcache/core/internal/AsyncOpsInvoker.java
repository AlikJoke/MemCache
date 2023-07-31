package ru.joke.memcache.core.internal;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

final class AsyncOpsInvoker implements Closeable {

    private final ExecutorService asyncOpsPool;

    AsyncOpsInvoker(@Nonnegative int parallelism) {
        this.asyncOpsPool = Executors.newWorkStealingPool(parallelism);
    }

    @Nonnull
    <U> CompletableFuture<U> invoke(@Nonnull Supplier<U> operation) {
        return CompletableFuture.supplyAsync(operation, this.asyncOpsPool);
    }

    @Nonnull
    CompletableFuture<Void> invoke(@Nonnull Runnable operation) {
        return CompletableFuture.runAsync(operation, this.asyncOpsPool);
    }

    @Override
    public void close() {
        this.asyncOpsPool.close();
    }
}
