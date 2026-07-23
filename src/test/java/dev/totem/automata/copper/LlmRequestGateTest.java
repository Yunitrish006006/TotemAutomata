package dev.totem.automata.copper;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmRequestGateTest {
    @Test
    void pendingKeyAllowsExactlyOneConcurrentStarter() throws Exception {
        int attempts = 32;
        LlmRequestGate gate = new LlmRequestGate(60_000L, () -> 1_000L);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return gate.tryStart("same-query");
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(5, TimeUnit.SECONDS)) accepted++;
            }
            assertEquals(1, accepted);
            assertTrue(gate.isPending("same-query"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void successReleasesPendingKeyImmediately() {
        LlmRequestGate gate = new LlmRequestGate(60_000L, () -> 10L);
        assertTrue(gate.tryStart("query"));
        assertFalse(gate.tryStart("query"));
        gate.completeSuccess("query");
        assertFalse(gate.isPending("query"));
        assertEquals(0L, gate.retryAfter("query"));
        assertTrue(gate.tryStart("query"));
    }

    @Test
    void failureBlocksUntilRetryDeadlineThenAllowsRetry() {
        AtomicLong clock = new AtomicLong(1_000L);
        LlmRequestGate gate = new LlmRequestGate(60_000L, clock::get);
        assertTrue(gate.tryStart("query"));
        gate.completeFailure("query");
        assertEquals(61_000L, gate.retryAfter("query"));
        assertFalse(gate.tryStart("query"));
        clock.set(60_999L);
        assertFalse(gate.tryStart("query"));
        clock.set(61_000L);
        assertTrue(gate.tryStart("query"));
    }

    @Test
    void cancellationAndClearReleaseAllState() {
        AtomicLong clock = new AtomicLong(1_000L);
        LlmRequestGate gate = new LlmRequestGate(60_000L, clock::get);
        assertTrue(gate.tryStart("pending"));
        gate.cancel("pending");
        assertTrue(gate.tryStart("failed"));
        gate.completeFailure("failed");
        gate.clear();
        assertFalse(gate.isPending("pending"));
        assertEquals(0L, gate.retryAfter("failed"));
        assertTrue(gate.tryStart("pending"));
        assertTrue(gate.tryStart("failed"));
    }
}
