package com.niranjankagri.booking;

import java.util.concurrent.atomic.AtomicInteger;

/** An event with a limited number of seats. Safe to use from many threads at once. */
final class Event {

    private final String id;
    private final String name;
    private final int capacity;
    private final AtomicInteger sold = new AtomicInteger();

    Event(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    /**
     * Takes seats if enough are left.
     *
     * @param seats how many
     * @return false when there aren't enough left
     */
    boolean reserve(int seats) {
        // Compare-and-set: "check, then add" as one step, so two bookings can't both take the last seats
        while (true) {
            int current = sold.get();
            if (current + seats > capacity) {
                return false;
            }
            if (sold.compareAndSet(current, current + seats)) {
                return true;
            }
        }
    }

    String id() {
        return id;
    }

    String name() {
        return name;
    }

    int capacity() {
        return capacity;
    }

    int sold() {
        return sold.get();
    }
}
