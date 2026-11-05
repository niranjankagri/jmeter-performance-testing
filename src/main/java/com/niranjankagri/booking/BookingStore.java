package com.niranjankagri.booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Events and bookings, in memory.
 *
 * <p>A booking needs one of a few "payment connections", held for 40-80 ms. That gives the API a real limit of about
 * 66 bookings per second, like a database or payment pool in a real system: the performance tests have something to find.
 */
final class BookingStore {

    /** The event with few seats. An odd number, so a race that sells past the limit can't land on it by luck. */
    static final String LIMITED_EVENT = "E-LIMITED";

    private static final int PAYMENT_CONNECTIONS = 4;

    private final Map<String, Event> events = new LinkedHashMap<>();
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    // Fair: requests are served in order of arrival, so waiting shows up as latency and not as starvation
    private final Semaphore paymentConnections = new Semaphore(PAYMENT_CONNECTIONS, true);

    BookingStore() {
        for (int i = 1; i <= 19; i++) {
            String id = String.format("E-%02d", i);
            events.put(id, new Event(id, "Concert " + i, 1_000_000));
        }
        events.put(LIMITED_EVENT, new Event(LIMITED_EVENT, "Sold-out show", 101));
    }

    List<Event> events() {
        pause(5, 15);
        return new ArrayList<>(events.values());
    }

    Optional<Event> event(String id) {
        pause(2, 8);
        return Optional.ofNullable(events.get(id));
    }

    /**
     * Books seats.
     *
     * @param event    the event
     * @param seats    how many seats
     * @param username who is booking
     * @return the booking, or empty when the event has too few seats left
     * @throws InterruptedException if the thread is stopped while waiting for a connection
     */
    Optional<Booking> book(Event event, int seats, String username) throws InterruptedException {
        paymentConnections.acquire();
        try {
            pause(40, 80);
            if (!event.reserve(seats)) {
                return Optional.empty();
            }
            Booking booking = new Booking(UUID.randomUUID().toString(), event.id(), seats, username);
            bookings.put(booking.id(), booking);
            return Optional.of(booking);
        } finally {
            paymentConnections.release();
        }
    }

    Optional<Booking> booking(String id) {
        pause(2, 8);
        return Optional.ofNullable(bookings.get(id));
    }

    private static void pause(int minMillis, int maxMillis) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMillis, maxMillis + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
