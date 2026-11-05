package com.niranjankagri.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.niranjankagri.booking.ApiClient.Answer;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The API works before anyone puts load on it: a load test of a broken API measures nothing. */
@DisplayName("Booking API")
class BookingApiTest {

    private BookingServer server;
    private ApiClient api;

    @BeforeEach
    void startServer() throws IOException {
        // Port 0: the operating system picks a free port
        server = new BookingServer(0, new BookingStore(), new UserDirectory());
        server.start();
        api = new ApiClient(server.port());
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void tc101_healthAnswersWithoutLogin() {
        // When
        Answer answer = api.get("/health", null);

        // Then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body().path("status").asText()).isEqualTo("UP");
    }

    @Test
    void tc102_aValidLoginGivesAToken() {
        // When
        Answer answer = api.login("user001", "pass-001");

        // Then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body().path("token").asText()).isNotBlank();
    }

    @Test
    void tc103_aWrongPasswordIsRefused() {
        // When / Then
        assertThat(api.login("user001", "pass-002").status()).isEqualTo(401);
    }

    @Test
    void tc104_theListHoldsAllTwentyEvents() {
        // When
        Answer answer = api.get("/events", null);

        // Then: the JMeter plan picks a random id from this list
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body()).hasSize(20);
    }

    @Test
    void tc105_anUnknownEventIsNotFound() {
        // When / Then
        assertThat(api.get("/events/E-99", null).status()).isEqualTo(404);
    }

    @Test
    void tc106_bookingWithoutATokenIsRefused() {
        // When / Then
        assertThat(api.book("E-01", 2, null).status()).isEqualTo(401);
    }

    @Test
    void tc107_aBookingCanBeReadBack() {
        // Given
        String token = api.tokenFor("user001");

        // When
        Answer created = api.book("E-01", 2, token);
        Answer read = api.get("/bookings/" + created.body().path("id").asText(), token);

        // Then
        assertThat(created.status()).isEqualTo(201);
        assertThat(read.status()).isEqualTo(200);
        assertThat(read.body().path("seats").asInt()).isEqualTo(2);
    }

    @Test
    void tc108_anotherUsersBookingIsNotShown() {
        // Given: a booking of user001
        String id = api.book("E-01", 2, api.tokenFor("user001")).body().path("id").asText();

        // When / Then: 404, not 403, so the answer doesn't confirm that the id exists
        assertThat(api.get("/bookings/" + id, api.tokenFor("user002")).status()).isEqualTo(404);
    }

    @Test
    void tc109_aBadSeatCountIsRefused() {
        // Given
        String token = api.tokenFor("user001");

        // When / Then
        assertThat(api.book("E-01", 0, token).status()).isEqualTo(400);
        assertThat(api.book("E-01", 5, token).status()).isEqualTo(400);
    }

    @Test
    void tc110_invalidJsonIsABadRequest() {
        // When / Then
        assertThat(api.post("/login", "not json", null).status()).isEqualTo(400);
    }

    @Test
    void tc111_theLastSeatsCanOnlyBeSoldOnce() {
        // Given: 101 seats, so 25 bookings of 4 leave one seat
        String token = api.tokenFor("user001");
        for (int i = 0; i < 25; i++) {
            assertThat(api.book(BookingStore.LIMITED_EVENT, 4, token).status()).isEqualTo(201);
        }

        // When / Then: 2 seats don't fit, 1 does, and then nothing is left
        assertThat(api.book(BookingStore.LIMITED_EVENT, 2, token).status()).isEqualTo(409);
        assertThat(api.book(BookingStore.LIMITED_EVENT, 1, token).status()).isEqualTo(201);
        assertThat(api.get("/events/" + BookingStore.LIMITED_EVENT, null).body().path("sold").asInt()).isEqualTo(101);
    }
}
