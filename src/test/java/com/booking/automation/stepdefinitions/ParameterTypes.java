package com.booking.automation.stepdefinitions;

import com.booking.automation.models.SortOption;
import io.cucumber.java.ParameterType;

/** Business vocabulary in Gherkin -> domain types in Java. */
public class ParameterTypes {

    @ParameterType("price low to high|price high to low|rating|recommended")
    public SortOption sortOrder(String phrase) {
        return switch (phrase) {
            case "price low to high" -> SortOption.PRICE_LOW_TO_HIGH;
            case "price high to low" -> SortOption.PRICE_HIGH_TO_LOW;
            case "rating" -> SortOption.RATING_HIGH_TO_LOW;
            case "recommended" -> SortOption.RECOMMENDED;
            default -> throw new IllegalArgumentException("Unknown sort order: " + phrase);
        };
    }
}
