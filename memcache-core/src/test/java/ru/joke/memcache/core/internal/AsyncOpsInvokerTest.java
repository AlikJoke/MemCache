package ru.joke.memcache.core.internal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncOpsInvokerTest {

    private AsyncOpsInvoker asyncOpsInvoker;

    @BeforeEach
    void setUp() {
        asyncOpsInvoker = new AsyncOpsInvoker(4);
    }

    @Test
    public void testInvocationWithReturnValue() throws ExecutionException, InterruptedException {
        final int value = 42;
        final Supplier<Integer> operation = () -> value;
        final CompletableFuture<Integer> future = this.asyncOpsInvoker.invoke(operation);

        assertNotNull(future, "Future must be not null");
        assertEquals(value, future.get(), "Result value must be equal");
    }

    @Test
    public void testInvocationWithoutReturnValue() throws ExecutionException, InterruptedException {
        final AtomicInteger value = new AtomicInteger();
        final Runnable operation = () -> value.set(2);

        final CompletableFuture<Void> future = asyncOpsInvoker.invoke(operation);

        assertNotNull(future, "Future must be not null");
        assertNull(future.get(), "Result must be void");
        assertEquals(2, value.get(), "Action should be performed");
    }

    @AfterEach
    void tearDown() {
        asyncOpsInvoker.close();
    }
}
