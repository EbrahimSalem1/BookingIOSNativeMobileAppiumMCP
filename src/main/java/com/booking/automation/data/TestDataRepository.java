package com.booking.automation.data;

import com.booking.automation.config.PlaceholderResolver;
import com.booking.automation.constants.FrameworkConstants;
import com.booking.automation.models.Credentials;
import com.booking.automation.models.FilterCriteria;
import com.booking.automation.models.SearchCriteria;
import com.booking.automation.models.SelectionStrategy;
import com.booking.automation.models.SortOption;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single entry point for test data. Feature files and step definitions refer to data by
 * <b>alias</b> ("valid", "paris_weekend"); the values live in {@code src/test/resources/testdata/*.json}.
 *
 * <ul>
 *     <li>Changing a username, destination, filter or sort label never requires a code change.</li>
 *     <li>Secrets are {@code ${ENV_VAR}} placeholders resolved at runtime (CI secrets / local .env).</li>
 *     <li>Each domain file declares a {@code default} alias used by the generic, readable steps
 *     ("searches using valid search criteria").</li>
 * </ul>
 */
public final class TestDataRepository {

    private static final TestDataRepository INSTANCE = new TestDataRepository();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, JsonNode> cache = new ConcurrentHashMap<>();

    private TestDataRepository() {
    }

    public static TestDataRepository get() {
        return INSTANCE;
    }

    public Credentials credentials(String alias) {
        Credentials credentials = dataset("users", alias, Credentials.class);
        if (credentials.username() == null || credentials.username().isBlank()) {
            throw new TestDataException("Username for '" + alias + "' is empty - set BOOKING_USERNAME / BOOKING_PASSWORD"
                    + " (see README > Environment setup)");
        }
        return credentials;
    }

    public SearchCriteria search(String alias) {
        return dataset("search", alias, SearchCriteria.class);
    }

    public SearchCriteria defaultSearch() {
        return search(defaultAlias("search"));
    }

    public FilterCriteria filter(String alias) {
        return dataset("filters", alias, FilterCriteria.class);
    }

    public FilterCriteria defaultFilter() {
        return filter(defaultAlias("filters"));
    }

    public SortOption defaultSortOption() {
        return SortOption.valueOf(defaultAlias("sorting"));
    }

    /** The app's wording for a sort option - copy changes are a data change, not a code change. */
    public String sortLabel(SortOption option) {
        JsonNode label = file("sorting").path("labels").path(option.name());
        if (label.isMissingNode()) {
            throw new TestDataException("No UI label configured for sort option " + option + " in sorting.json");
        }
        return label.asText();
    }

    public SelectionStrategy selectionStrategy() {
        return SelectionStrategy.valueOf(file("selection").path("strategy").asText("FIRST_BOOKABLE"));
    }

    public String expectedMessage(String key) {
        JsonNode node = file("messages").path(key);
        if (node.isMissingNode()) {
            throw new TestDataException("No expected message '" + key + "' in messages.json");
        }
        return node.asText();
    }

    // ------------------------------------------------------------------ internals

    private <T> T dataset(String file, String alias, Class<T> type) {
        JsonNode node = file(file).path("datasets").path(alias);
        if (node.isMissingNode()) {
            throw new TestDataException("Unknown " + file + " dataset '" + alias + "'. Available: "
                    + iterableToString(file(file).path("datasets").fieldNames()));
        }
        try {
            return mapper.treeToValue(node, type);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new TestDataException("Dataset '" + alias + "' in " + file + ".json is invalid: " + e.getMessage(), e);
        }
    }

    private String defaultAlias(String file) {
        String alias = file(file).path("default").asText("");
        if (alias.isBlank()) {
            throw new TestDataException(file + ".json must declare a \"default\" alias");
        }
        return alias;
    }

    private JsonNode file(String name) {
        return cache.computeIfAbsent(name, this::load);
    }

    private JsonNode load(String name) {
        String resource = FrameworkConstants.TEST_DATA_DIR + name + ".json";
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new TestDataException("Test data file not found on classpath: " + resource);
            }
            return resolvePlaceholders(mapper.readTree(in));
        } catch (IOException e) {
            throw new TestDataException("Cannot read " + resource, e);
        }
    }

    private static JsonNode resolvePlaceholders(JsonNode node) {
        if (node instanceof ObjectNode object) {
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getValue().isTextual()) {
                    field.setValue(TextNode.valueOf(String.valueOf(PlaceholderResolver.resolve(field.getValue().asText()))));
                } else {
                    resolvePlaceholders(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(TestDataRepository::resolvePlaceholders);
        }
        return node;
    }

    private static String iterableToString(Iterator<String> names) {
        StringBuilder sb = new StringBuilder();
        names.forEachRemaining(n -> sb.append(sb.length() == 0 ? "" : ", ").append(n));
        return sb.toString();
    }

    public static class TestDataException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        public TestDataException(String message) {
            super(message);
        }

        public TestDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
