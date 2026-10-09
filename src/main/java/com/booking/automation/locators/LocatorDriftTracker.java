package com.booking.automation.locators;

import com.booking.automation.constants.FrameworkConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Early-warning system for UI changes.
 *
 * <p>Every time an element is found by a <em>fallback</em> strategy instead of the primary one, the
 * locator is recorded here. At the end of the run a JSON report is written to
 * {@code target/locator-drift-report.json}; it is attached to Allure and fed to the AI failure
 * analyst, which proposes the updated primary locator (verified via Appium MCP before merging).</p>
 */
public final class LocatorDriftTracker {

    private static final Logger LOG = LoggerFactory.getLogger(LocatorDriftTracker.class);
    private static final Map<String, DriftEvent> EVENTS = new ConcurrentHashMap<>();

    private LocatorDriftTracker() {
    }

    static void recordFallback(Locator locator, int strategyIndex, String screen) {
        EVENTS.compute(locator.name(), (name, existing) -> {
            DriftEvent event = existing != null ? existing
                    : new DriftEvent(name, locator.primary().toString(),
                    locator.strategies().get(strategyIndex).toString(), screen, Instant.now().toString(), new AtomicInteger());
            event.occurrences().incrementAndGet();
            return event;
        });
        LOG.warn("LOCATOR DRIFT: '{}' not found by primary {}, matched fallback #{} {}",
                locator.name(), locator.primary(), strategyIndex, locator.strategies().get(strategyIndex));
    }

    public static Collection<DriftEvent> events() {
        return List.copyOf(EVENTS.values());
    }

    public static Path writeReport() {
        Path report = FrameworkConstants.LOCATOR_DRIFT_REPORT;
        try {
            Files.createDirectories(report.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(report.toFile(), new ArrayList<>(EVENTS.values()));
            if (!EVENTS.isEmpty()) {
                LOG.warn("{} locator(s) drifted - see {}", EVENTS.size(), report.toAbsolutePath());
            }
        } catch (IOException e) {
            LOG.error("Could not write locator drift report", e);
        }
        return report;
    }

    public record DriftEvent(String locator, String brokenPrimary, String matchedFallback,
                             String screen, String firstSeen, AtomicInteger occurrences) {
    }
}
