package com.niranjankagri.booking;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** The 200 test users of src/test/jmeter/users.csv (user001 / pass-001), and the tokens handed out at login. */
final class UserDirectory {

    private final Map<String, String> usernameByToken = new ConcurrentHashMap<>();

    /**
     * @param username the username
     * @param password the password
     * @return a new token, or empty when the login is wrong
     */
    Optional<String> login(String username, String password) {
        if (username == null || !username.matches("user\\d{3}")) {
            return Optional.empty();
        }
        if (!("pass-" + username.substring(4)).equals(password)) {
            return Optional.empty();
        }
        String token = UUID.randomUUID().toString();
        usernameByToken.put(token, username);
        return Optional.of(token);
    }

    /**
     * @param token a token from {@link #login}
     * @return the user it belongs to, or empty when it is unknown
     */
    Optional<String> usernameFor(String token) {
        return Optional.ofNullable(usernameByToken.get(token));
    }
}
