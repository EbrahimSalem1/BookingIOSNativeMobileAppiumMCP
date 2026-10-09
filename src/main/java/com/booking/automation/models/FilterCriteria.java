package com.booking.automation.models;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A filter the user applies, expressed as data (loaded from test data) plus the business rule
 * every remaining result must satisfy.
 *
 * @param type      the kind of filter, which defines the verification rule
 * @param uiLabel   the label the user taps in the filter sheet (the app's wording, from test data)
 * @param value     the threshold / expected value, e.g. "200" for PRICE_MAX or "Hotel" for PROPERTY_TYPE
 */
public record FilterCriteria(Type type, String uiLabel, String value) {

    public enum Type { PRICE_MAX, MIN_RATING, PROPERTY_TYPE, LOCATION, AVAILABLE_ONLY }

    public FilterCriteria {
        Objects.requireNonNull(type, "filter type");
        Objects.requireNonNull(uiLabel, "filter uiLabel");
    }

    /** The oracle: true when a result complies with this filter. */
    public Predicate<ResultItem> rule() {
        return switch (type) {
            case PRICE_MAX -> item -> item.price().map(p -> p.compareTo(new BigDecimal(value)) <= 0).orElse(false);
            case MIN_RATING -> item -> item.rating().map(r -> r >= Double.parseDouble(value)).orElse(false);
            case PROPERTY_TYPE -> item -> item.propertyType().equalsIgnoreCase(value);
            case LOCATION -> item -> item.location().toLowerCase().contains(value.toLowerCase());
            case AVAILABLE_ONLY -> ResultItem::available;
        };
    }

    public String describe() {
        return type + " '" + uiLabel + "'" + (value == null ? "" : " (" + value + ")");
    }
}
