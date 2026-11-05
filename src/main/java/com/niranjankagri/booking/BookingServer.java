package com.niranjankagri.booking;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/** The HTTP API, on the JDK's own server: no framework, so the only limit in play is the one built in on purpose. */
final class BookingServer {

    private static final ObjectMapper JSON = new ObjectMapper();
    // How many connections may wait to be accepted; the JDK's default is too small for a load test
    private static final int BACKLOG = 1024;

    private final HttpServer server;
    private final BookingStore store;
    private final UserDirectory users;

    BookingServer(int port, BookingStore store, UserDirectory users) throws IOException {
        this.store = store;
        this.users = users;
        server = HttpServer.create(new InetSocketAddress(port), BACKLOG);
        server.createContext("/", this::handle);
        // Virtual threads: a request waiting for a payment connection costs almost nothing
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    void start() {
        server.start();
    }

    void stop() {
        server.stop(0);
    }

    int port() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        Reply reply;
        try {
            reply = route(exchange);
        } catch (JsonProcessingException e) {
            reply = error(400, "invalid JSON");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reply = error(503, "interrupted");
        }
        byte[] body = JSON.writeValueAsBytes(reply.body());
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), body.length);
        try (var out = exchange.getResponseBody()) {
            out.write(body);
        }
    }

    private Reply route(HttpExchange exchange) throws IOException, InterruptedException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(method) && "/health".equals(path)) {
            return new Reply(200, Map.of("status", "UP"));
        }
        if ("POST".equals(method) && "/login".equals(path)) {
            return login(exchange);
        }
        if ("GET".equals(method) && "/events".equals(path)) {
            return new Reply(200, store.events().stream().map(BookingServer::summary).toList());
        }
        if ("GET".equals(method) && path.startsWith("/events/")) {
            return store.event(path.substring("/events/".length()))
                    .map(event -> new Reply(200, summary(event)))
                    .orElse(error(404, "no such event"));
        }
        if (path.equals("/bookings") || path.startsWith("/bookings/")) {
            return bookings(exchange, method, path);
        }
        return error(404, "not found");
    }

    private Reply login(HttpExchange exchange) throws IOException {
        JsonNode body = readBody(exchange);
        return users.login(body.path("username").asText(null), body.path("password").asText(null))
                .map(token -> new Reply(200, Map.of("token", token)))
                .orElse(error(401, "wrong username or password"));
    }

    private Reply bookings(HttpExchange exchange, String method, String path) throws IOException, InterruptedException {
        Optional<String> username = authenticatedUser(exchange);
        if (username.isEmpty()) {
            return error(401, "a valid token is required");
        }
        if ("POST".equals(method) && "/bookings".equals(path)) {
            return createBooking(readBody(exchange), username.get());
        }
        if ("GET".equals(method) && path.startsWith("/bookings/")) {
            return store.booking(path.substring("/bookings/".length()))
                    .filter(booking -> booking.username().equals(username.get()))
                    .map(booking -> new Reply(200, booking))
                    .orElse(error(404, "no such booking"));
        }
        return error(404, "not found");
    }

    private Reply createBooking(JsonNode body, String username) throws InterruptedException {
        int seats = body.path("seats").asInt(0);
        if (seats < 1 || seats > 4) {
            return error(400, "seats must be 1 to 4");
        }
        Optional<Event> event = store.event(body.path("eventId").asText(""));
        if (event.isEmpty()) {
            return error(404, "no such event");
        }
        return store.book(event.get(), seats, username)
                .map(booking -> new Reply(201, booking))
                .orElse(error(409, "not enough seats left"));
    }

    private Optional<String> authenticatedUser(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return users.usernameFor(header.substring("Bearer ".length()));
    }

    private static JsonNode readBody(HttpExchange exchange) throws IOException {
        return JSON.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static Map<String, Object> summary(Event event) {
        return Map.of("id", event.id(), "name", event.name(), "capacity", event.capacity(), "sold", event.sold());
    }

    private static Reply error(int status, String message) {
        return new Reply(status, Map.of("error", message));
    }

    private record Reply(int status, Object body) {
    }
}
