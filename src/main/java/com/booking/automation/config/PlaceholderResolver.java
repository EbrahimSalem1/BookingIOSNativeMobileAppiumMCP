package com.booking.automation.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${NAME}} and {@code ${NAME:-fallback}} placeholders against environment
 * variables first, then JVM system properties. Keeps secrets out of version control.
 */
public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Za-z0-9_.]+)(?::-([^}]*))?}");

    private PlaceholderResolver() {
    }

    public static Object resolve(Object value) {
        if (!(value instanceof String text) || !text.contains("${")) {
            return value;
        }
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String fallback = matcher.group(2);
            String resolved = System.getenv(name);
            if (resolved == null) {
                resolved = System.getProperty(name);
            }
            if (resolved == null) {
                resolved = fallback == null ? "" : fallback;
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
