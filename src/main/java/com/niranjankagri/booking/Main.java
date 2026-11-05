package com.niranjankagri.booking;

import java.io.IOException;

/** Starts the API. */
public final class Main {

    private Main() {
    }

    /**
     * @param args the port, 8080 if left out
     * @throws IOException if the port can't be opened
     */
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        BookingServer server = new BookingServer(port, new BookingStore(), new UserDirectory());
        server.start();
        System.out.println("Booking API on http://localhost:" + server.port());
    }
}
