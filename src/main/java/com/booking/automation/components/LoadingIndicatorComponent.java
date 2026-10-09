package com.booking.automation.components;

import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.utils.Waits;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * Spinners and skeleton loaders. Waiting for the loader to <em>disappear</em> - rather than for a
 * fixed time - is what keeps login and search stable on slow CI simulators.
 */
public class LoadingIndicatorComponent {

    private static final Locator LOADER = Locator.named("common.loadingIndicator")
            .accessibilityId("loading_indicator")
            .predicate("type == 'XCUIElementTypeActivityIndicator' AND visible == 1")
            .build();

    private final ElementFinder find;

    public LoadingIndicatorComponent(IOSDriver driver) {
        this.find = new ElementFinder(driver, "LoadingIndicator");
    }

    public boolean isShown() {
        return find.findNow(LOADER).map(WebElement::isDisplayed).orElse(false);
    }

    /** Returns immediately if no loader is present; otherwise waits for it to go away. */
    public void waitUntilGone(Duration timeout) {
        Waits.untilTrue(() -> !isShown(), timeout, "loading indicator to disappear");
    }
}
