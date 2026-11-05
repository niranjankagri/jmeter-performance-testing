package com.niranjankagri.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

/** The seat count under real concurrency, without the API around it. */
@DisplayName("Seat reservation")
class EventTest {

    private static final int THREADS = 64;

    // Repeated: a race can hide in a single run
    @RepeatedTest(20)
    void tc121_parallelBookingsNeverSellPastTheCapacity() throws Exception {
        // Given: 101 seats and 64 threads taking 2 seats, 10 times each
        Event event = new Event("E-X", "Test", 101);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> taken = new ArrayList<>();

        // When: all threads wait for one signal, so they really collide
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                taken.add(pool.submit(() -> {
                    start.await();
                    int seats = 0;
                    for (int i = 0; i < 10; i++) {
                        seats += event.reserve(2) ? 2 : 0;
                    }
                    return seats;
                }));
            }
            start.countDown();
        }

        // Then: exactly 100 sold, because one seat can't fit a booking of 2, and every seat was counted once
        int total = 0;
        for (Future<Integer> future : taken) {
            total += future.get();
        }
        assertThat(event.sold()).isEqualTo(100);
        assertThat(total).isEqualTo(100);
    }
}
