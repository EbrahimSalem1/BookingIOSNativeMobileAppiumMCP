package com.booking.automation.models;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * A search result as the user sees it on a result card. Captured before tapping a result so the
 * details screen can be verified against <em>what was selected</em>, not against static test data.
 *
 * @param position     zero-based position in the list at capture time
 * @param name         property name (the identity used to de-duplicate while scrolling)
 * @param location     location / address line
 * @param price        parsed price, empty when not shown (e.g. sold out)
 * @param rating       rating normalised to 0-5, empty when unrated
 * @param propertyType e.g. Hotel, Apartment - empty if the card does not show it
 * @param available    false if the card shows a sold-out / unavailable badge
 */
public record ResultItem(int position, String name, String location, Optional<BigDecimal> price,
                         Optional<Double> rating, String propertyType, boolean available) {

    public boolean isBookable() {
        return available && price.isPresent() && name != null && !name.isBlank();
    }

    @Override
    public String toString() {
        return "#" + position + " '" + name + "' (" + location + ") price=" + price.map(BigDecimal::toPlainString).orElse("-")
                + " rating=" + rating.map(String::valueOf).orElse("-") + (available ? "" : " [unavailable]");
    }
}
