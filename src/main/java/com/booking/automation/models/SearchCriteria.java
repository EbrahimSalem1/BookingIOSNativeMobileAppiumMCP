package com.booking.automation.models;

import java.time.LocalDate;
import java.util.Objects;

/**
 * What the user searches for. Dates are expressed as offsets from "today" so test data never
 * expires (a hard-coded check-in date in the past is a classic source of false failures).
 *
 * @param destination      free text typed by the user, e.g. "Paris"
 * @param relevanceKeyword the text every result must relate to; defaults to the destination
 * @param checkInInDays    days from today
 * @param nights           length of stay
 * @param adults           number of adult guests
 * @param rooms            number of rooms
 */
public record SearchCriteria(String destination, String relevanceKeyword, int checkInInDays, int nights,
                             int adults, int rooms) {

    public SearchCriteria {
        Objects.requireNonNull(destination, "destination");
        if (relevanceKeyword == null || relevanceKeyword.isBlank()) {
            relevanceKeyword = destination;
        }
        if (nights < 1 || adults < 1 || rooms < 1 || checkInInDays < 0) {
            throw new IllegalArgumentException("Invalid search criteria: " + destination + " nights=" + nights
                    + " adults=" + adults + " rooms=" + rooms + " checkInInDays=" + checkInInDays);
        }
    }

    public LocalDate checkIn() {
        return LocalDate.now().plusDays(checkInInDays);
    }

    public LocalDate checkOut() {
        return checkIn().plusDays(nights);
    }
}
