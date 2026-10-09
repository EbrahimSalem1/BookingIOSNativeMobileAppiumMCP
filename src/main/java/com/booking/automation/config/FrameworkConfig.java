package com.booking.automation.config;

import com.booking.automation.constants.FrameworkConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Layered, immutable framework configuration.
 *
 * <p>Resolution order (later wins):</p>
 * <ol>
 *     <li>{@code config/default.yaml} - safe defaults shared by every environment</li>
 *     <li>{@code config/<env>.yaml} - environment profile (local, ci, cloud, ...)</li>
 *     <li>Environment variables - {@code device.udid} is read from {@code DEVICE_UDID}</li>
 *     <li>JVM system properties - {@code -Ddevice.udid=...}</li>
 * </ol>
 *
 * <p>Only keys declared in YAML can be overridden. This is deliberate: a typo such as
 * {@code -Ddevice.uuid} fails loudly via {@link #getString(String)} instead of being silently ignored.</p>
 *
 * <p>Secrets (credentials, cloud keys) are never committed: YAML files reference them as
 * {@code ${ENV_VAR}} placeholders which are resolved at load time.</p>
 */
public final class FrameworkConfig {

    private static final Logger LOG = LoggerFactory.getLogger(FrameworkConfig.class);
    private static volatile FrameworkConfig instance;

    private final String environment;
    private final Map<String, Object> values;

    FrameworkConfig(String environment, Map<String, Object> values) {
        this.environment = environment;
        this.values = Collections.unmodifiableMap(values);
    }

    public static FrameworkConfig get() {
        if (instance == null) {
            synchronized (FrameworkConfig.class) {
                if (instance == null) {
                    instance = load(System.getProperty(FrameworkConstants.ENV_PROPERTY,
                            System.getenv().getOrDefault("TEST_ENV", FrameworkConstants.DEFAULT_ENV)));
                }
            }
        }
        return instance;
    }

    static FrameworkConfig load(String env) {
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(flatten("", readYaml(FrameworkConstants.CONFIG_DIR + "default.yaml", true)));
        merged.putAll(flatten("", readYaml(FrameworkConstants.CONFIG_DIR + env + ".yaml", false)));

        merged.replaceAll((key, value) -> {
            Object override = Optional.<Object>ofNullable(System.getProperty(key))
                    .or(() -> Optional.ofNullable(System.getenv(toEnvName(key))))
                    .orElse(value);
            return PlaceholderResolver.resolve(override);
        });
        LOG.info("Loaded configuration for environment '{}' ({} keys)", env, merged.size());
        return new FrameworkConfig(env, merged);
    }

    // ---------------------------------------------------------------- typed accessors

    public String environment() {
        return environment;
    }

    public String getString(String key) {
        return optionalString(key).orElseThrow(() ->
                new ConfigurationException("Missing required configuration key '" + key
                        + "' for environment '" + environment + "'"));
    }

    public Optional<String> optionalString(String key) {
        Object value = values.get(key);
        if (value == null || value.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value.toString().trim());
    }

    public String getString(String key, String defaultValue) {
        return optionalString(key).orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return optionalString(key).map(v -> parse(key, v, Integer::parseInt)).orElse(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return optionalString(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public Duration getSeconds(String key, long defaultSeconds) {
        return Duration.ofSeconds(optionalString(key)
                .map(v -> parse(key, v, Long::parseLong))
                .orElse(defaultSeconds));
    }

    public Duration getMillis(String key, long defaultMillis) {
        return Duration.ofMillis(optionalString(key)
                .map(v -> parse(key, v, Long::parseLong))
                .orElse(defaultMillis));
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E defaultValue) {
        return optionalString(key)
                .map(v -> Enum.valueOf(type, v.trim().toUpperCase(Locale.ROOT)))
                .orElse(defaultValue);
    }

    /** All keys under a prefix, with the prefix stripped, e.g. {@code capabilities.extra.}. */
    public Map<String, Object> subset(String prefix) {
        Map<String, Object> result = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), value);
            }
        });
        return result;
    }

    /** Read-only view for reporting (e.g. Allure environment widget). Secrets are masked. */
    public Map<String, String> safeSnapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        values.forEach((key, value) -> snapshot.put(key, isSecret(key) ? "******" : String.valueOf(value)));
        return snapshot;
    }

    // ---------------------------------------------------------------- loading helpers

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readYaml(String resource, boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new ConfigurationException("Required config resource not found: " + resource);
                }
                LOG.warn("Optional config resource not found: {}", resource);
                return Map.of();
            }
            Map<String, Object> map = new ObjectMapper(new YAMLFactory()).readValue(in, Map.class);
            return map == null ? Map.of() : map;
        } catch (IOException e) {
            throw new ConfigurationException("Cannot parse config resource " + resource, e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> flatten(String prefix, Map<String, Object> source) {
        Map<String, Object> flat = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map<?, ?> nested) {
                flat.putAll(flatten(fullKey, (Map<String, Object>) nested));
            } else if (value instanceof List<?> list) {
                flat.put(fullKey, String.join(",", list.stream().map(String::valueOf).toList()));
            } else {
                flat.put(fullKey, value);
            }
        });
        return flat;
    }

    static String toEnvName(String key) {
        return key.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static boolean isSecret(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return k.contains("password") || k.contains("secret") || k.contains("token")
                || (k.contains("key") && !k.contains("keyboard"));
    }

    private static <T> T parse(String key, String raw, java.util.function.Function<String, T> parser) {
        try {
            return parser.apply(raw.trim());
        } catch (RuntimeException e) {
            throw new ConfigurationException("Invalid value '" + raw + "' for key '" + key + "'", e);
        }
    }
}
