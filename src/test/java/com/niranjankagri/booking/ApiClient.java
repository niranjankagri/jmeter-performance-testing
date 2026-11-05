package com.niranjankagri.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** The API as tests see it: paths and JSON live here, so tests read as behaviour. */
final class ApiClient {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    /** One answer: its status and its JSON. */
    record Answer(int status, JsonNode body) {
    }

    ApiClient(int port) {
        baseUrl = "http://localhost:" + port;
    }

    Answer login(String username, String password) {
        return post("/login", "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}", null);
    }

    /** Logs in a valid user and returns the token. */
    String tokenFor(String username) {
        return login(username, "pass-" + username.substring(4)).body().path("token").asText();
    }

    Answer book(String eventId, int seats, String token) {
        return post("/bookings", "{\"eventId\":\"" + eventId + "\",\"seats\":" + seats + "}", token);
    }

    Answer post(String path, String body, String token) {
        return send(request(path, token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    Answer get(String path, String token) {
        return send(request(path, token).GET());
    }

    private HttpRequest.Builder request(String path, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path));
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }

    private Answer send(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Answer(response.statusCode(), JSON.readTree(response.body()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
