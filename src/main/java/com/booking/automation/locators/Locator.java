package com.booking.automation.locators;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A named element locator with an ordered list of strategies.
 *
 * <p>Strategy order encodes the team's locator policy (fastest + most stable first):</p>
 * <ol>
 *     <li><b>Accessibility id</b> - contract with the iOS developers; survives layout and copy changes.</li>
 *     <li><b>NSPredicate / Class chain</b> - native XCUITest queries, executed inside WDA, fast.</li>
 *     <li><b>XPath</b> - last resort only; slow on iOS because WDA must serialise the whole tree.</li>
 * </ol>
 *
 * <p>When the primary strategy fails but a fallback succeeds, the test continues and the event is
 * recorded by {@link LocatorDriftTracker} so changed locators are detected <em>before</em> they break
 * the suite.</p>
 */
public final class Locator {

    private final String name;
    private final List<By> strategies;

    private Locator(String name, List<By> strategies) {
        this.name = Objects.requireNonNull(name);
        this.strategies = Collections.unmodifiableList(strategies);
    }

    public static Builder named(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public By primary() {
        return strategies.get(0);
    }

    public List<By> strategies() {
        return strategies;
    }

    /** Dynamic locator, e.g. a filter option whose label comes from test data. */
    public static Locator byLabel(String name, String label) {
        String safe = label.replace("'", "\\'");
        return named(name + "[" + label + "]")
                .predicate("label == '" + safe + "' OR name == '" + safe + "'")
                .predicate("label CONTAINS[c] '" + safe + "'")
                .build();
    }

    @Override
    public String toString() {
        return name + " " + strategies;
    }

    public static final class Builder {
        private final String name;
        private final List<By> strategies = new ArrayList<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder accessibilityId(String id) {
            strategies.add(AppiumBy.accessibilityId(id));
            return this;
        }

        public Builder predicate(String nsPredicate) {
            strategies.add(AppiumBy.iOSNsPredicateString(nsPredicate));
            return this;
        }

        public Builder classChain(String chain) {
            strategies.add(AppiumBy.iOSClassChain(chain));
            return this;
        }

        /** Deliberately verbose name: XPath should feel like an exception in code review. */
        public Builder xpathAsLastResort(String xpath) {
            strategies.add(By.xpath(xpath));
            return this;
        }

        public Locator build() {
            if (strategies.isEmpty()) {
                throw new IllegalStateException("Locator '" + name + "' has no strategies");
            }
            return new Locator(name, new ArrayList<>(strategies));
        }
    }
}
