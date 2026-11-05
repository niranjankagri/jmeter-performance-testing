package com.niranjankagri.gate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Waits until the API answers, so JMeter never starts against a server that is still starting. */
public final class WaitForApi {

    private static final int ATTEMPTS = 60;
    private static final long PAUSE_MILLIS = 500;

    private WaitForApi() {
    }

    /**
     * @param args the health URL
     * @throws InterruptedException if the wait is stopped
     */
    public static void main(String[] args) throws InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(args[0])).GET().build();
        try (HttpClient http = HttpClient.newHttpClient()) {
            for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
                try {
                    if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) {
                        return;
                    }
                } catch (IOException e) {
                    // Not listening yet: that's what this class waits for
                }
                Thread.sleep(PAUSE_MILLIS);
            }
        }
        throw new IllegalStateException("The API did not start: " + args[0]);
    }
}
