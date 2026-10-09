package com.booking.automation.models;

/** Login credentials. {@link #toString()} masks the password so it never reaches logs or reports. */
public record Credentials(String username, String password) {

    @Override
    public String toString() {
        return "Credentials[username=" + username + ", password=******]";
    }
}
