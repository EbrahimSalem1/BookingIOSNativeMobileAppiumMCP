package com.booking.automation.utils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts on-screen text into comparable values. Business assertions (sorting, filtering) are
 * made on parsed numbers, never on raw strings: "$1,200" sorts before "$300" as text.
 */
public final class TextParsers {

    // First number in the text, allowing thousand separators and decimals: "EGP 2,450.50 / night"
    private static final Pattern NUMBER = Pattern.compile("(\\d{1,3}(?:[,\\s\\u00A0]\\d{3})+|\\d+)(?:[.](\\d+))?");
    // "4.5", "8.7/10", "Rated 4.5 out of 5"
    private static final Pattern RATING = Pattern.compile("(\\d+(?:[.,]\\d+)?)(?:\\s*(?:/|out of)\\s*(\\d+))?");

    private TextParsers() {
    }

    public static Optional<BigDecimal> price(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher m = NUMBER.matcher(text);
        if (!m.find()) {
            return Optional.empty();
        }
        String integerPart = m.group(1).replaceAll("[,\\s\\u00A0]", "");
        String decimals = m.group(2);
        return Optional.of(new BigDecimal(decimals == null ? integerPart : integerPart + "." + decimals));
    }

    /**
     * Rating normalised to a 0-5 scale so that "8.6/10" and "4.3" compare correctly
     * if the app mixes review sources.
     */
    public static Optional<Double> rating(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher m = RATING.matcher(text);
        if (!m.find()) {
            return Optional.empty();
        }
        double value = Double.parseDouble(m.group(1).replace(',', '.'));
        String scale = m.group(2);
        if (scale != null) {
            double outOf = Double.parseDouble(scale);
            if (outOf > 0 && outOf != 5) {
                value = value * 5 / outOf;
            }
        }
        return Optional.of(Math.round(value * 100.0) / 100.0);
    }

    /** Case and whitespace insensitive "contains", for relevance checks against search criteria. */
    public static boolean containsIgnoringCase(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        return normalise(haystack).contains(normalise(needle));
    }

    public static String normalise(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
