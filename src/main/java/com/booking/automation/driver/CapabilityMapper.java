package com.booking.automation.driver;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns flattened config entries back into W3C capabilities:
 * <ul>
 *     <li>{@code "bstack:options.userName" -> {"bstack:options": {"userName": ...}}} (vendor option blocks)</li>
 *     <li>{@code "true"/"false"} and numeric strings become typed values (env/placeholder overrides are strings)</li>
 *     <li>blank values are dropped, so optional capabilities can be declared with {@code ${VAR:-}}</li>
 * </ul>
 */
final class CapabilityMapper {

    private CapabilityMapper() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> toCapabilities(Map<String, Object> flat) {
        Map<String, Object> caps = new LinkedHashMap<>();
        flat.forEach((key, rawValue) -> {
            Object value = typed(rawValue);
            if (value == null) {
                return;
            }
            int dot = key.indexOf('.');
            if (dot < 0) {
                caps.put(key, value);
            } else {
                Map<String, Object> block = (Map<String, Object>) caps.computeIfAbsent(
                        key.substring(0, dot), k -> new LinkedHashMap<String, Object>());
                block.put(key.substring(dot + 1), value);
            }
        });
        return caps;
    }

    static Object typed(Object raw) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String text)) {
            return raw;
        }
        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        if (value.matches("-?\\d{1,9}")) {
            return Integer.parseInt(value);
        }
        return value;
    }
}
