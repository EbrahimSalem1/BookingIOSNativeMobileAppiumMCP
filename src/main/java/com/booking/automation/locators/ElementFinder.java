package com.booking.automation.locators;

import com.booking.automation.utils.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Resolves {@link Locator}s against a driver or a parent element.
 *
 * <p>Each poll tries every strategy in order, so a drifted primary locator costs one cheap
 * miss per poll rather than a full timeout before the fallback is tried.</p>
 */
public final class ElementFinder {

    private final SearchContext context;
    private final String screen;

    public ElementFinder(SearchContext context, String screen) {
        this.context = context;
        this.screen = screen;
    }

    /** Waits for the first element matched by any strategy and checks it is displayed. */
    public WebElement visible(Locator locator, Duration timeout) {
        return Waits.until(() -> findNow(locator).filter(WebElement::isDisplayed).orElse(null),
                timeout, "'" + locator.name() + "' to be visible on " + screen);
    }

    /** Waits for presence in the hierarchy (may be off-screen, e.g. in a scroll view). */
    public WebElement present(Locator locator, Duration timeout) {
        return Waits.until(() -> findNow(locator).orElse(null),
                timeout, "'" + locator.name() + "' to be present on " + screen);
    }

    /** Waits until at least one element exists, then returns all matches of the winning strategy. */
    public List<WebElement> all(Locator locator, Duration timeout) {
        return Waits.until(() -> {
            List<WebElement> found = findAllNow(locator);
            return found.isEmpty() ? null : found;
        }, timeout, "at least one '" + locator.name() + "' on " + screen);
    }

    /** Single non-waiting probe across all strategies. */
    public Optional<WebElement> findNow(Locator locator) {
        List<By> strategies = locator.strategies();
        for (int i = 0; i < strategies.size(); i++) {
            List<WebElement> found = context.findElements(strategies.get(i));
            if (!found.isEmpty()) {
                if (i > 0) {
                    LocatorDriftTracker.recordFallback(locator, i, screen);
                }
                return Optional.of(found.get(0));
            }
        }
        return Optional.empty();
    }

    public List<WebElement> findAllNow(Locator locator) {
        List<By> strategies = locator.strategies();
        for (int i = 0; i < strategies.size(); i++) {
            List<WebElement> found = context.findElements(strategies.get(i));
            if (!found.isEmpty()) {
                if (i > 0) {
                    LocatorDriftTracker.recordFallback(locator, i, screen);
                }
                return found;
            }
        }
        return List.of();
    }

    /** For child elements of an already-resolved parent (e.g. fields inside a result card). */
    public String textOrEmpty(Locator locator) {
        return findNow(locator).map(ElementFinder::readText).orElse("");
    }

    public WebElement required(Locator locator) {
        return findNow(locator).orElseThrow(() ->
                new NoSuchElementException("'" + locator.name() + "' not found on " + screen + " using " + locator.strategies()));
    }

    /**
     * XCUITest exposes text through different attributes depending on element type: static texts use
     * {@code label}/{@code value}, text fields use {@code value}. Normalise here once.
     */
    public static String readText(WebElement element) {
        String label = element.getAttribute("label");
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        String value = element.getAttribute("value");
        return value == null ? "" : value.trim();
    }
}
