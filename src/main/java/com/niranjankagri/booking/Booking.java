package com.niranjankagri.booking;

/**
 * A confirmed booking.
 *
 * @param id       the booking's id
 * @param eventId  the event
 * @param seats    the number of seats
 * @param username who booked
 */
record Booking(String id, String eventId, int seats, String username) {
}
