package com.booking.automation.models;

import java.math.BigDecimal;
import java.util.Optional;

/** What the details screen shows, read from the UI for comparison with the selected {@link ResultItem}. */
public record BookingDetails(String name, String location, Optional<BigDecimal> price, Optional<Double> rating,
                             int imageCount, boolean bookActionAvailable) {
}
