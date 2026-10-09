package com.booking.automation.components;

import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.utils.Waits;
import io.appium.java_client.ios.IOSDriver;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * In-app and system alerts (permissions, "Save password?", error dialogs).
 * Uses XCUITest's {@code mobile: alert} which works for both SpringBoard and in-app UIAlertControllers.
 */
public class AlertComponent {

    private static final Locator ALERT = Locator.named("common.alert")
            .classChain("**/XCUIElementTypeAlert")
            .build();

    private final IOSDriver driver;
    private final ElementFinder find;

    public AlertComponent(IOSDriver driver) {
        this.driver = driver;
        this.find = new ElementFinder(driver, "Alert");
    }

    public Optional<String> textIfShown(Duration within) {
        if (!Waits.isTrueWithin(() -> find.findNow(ALERT).isPresent(), within)) {
            return Optional.empty();
        }
        return find.findNow(ALERT).map(ElementFinder::readText);
    }

    /** Taps the button with the given label, e.g. "Not Now" on the iOS save-password prompt. */
    public void tapButton(String label) {
        driver.executeScript("mobile: alert", Map.of("action", "accept", "buttonLabel", label));
    }

    public void dismissIfShown(String buttonLabel, Duration within) {
        if (textIfShown(within).isPresent()) {
            tapButton(buttonLabel);
        }
    }
}
