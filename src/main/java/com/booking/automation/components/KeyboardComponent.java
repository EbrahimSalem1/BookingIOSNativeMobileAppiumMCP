package com.booking.automation.components;

import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.utils.Waits;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;

/**
 * The iOS software keyboard.
 *
 * <p>{@code driver.hideKeyboard()} is unreliable on iOS (there is no system "hide" key on most
 * layouts), so dismissal tries the keyboard's own return/done key, then a toolbar "Done" button,
 * and only then falls back to {@code hideKeyboard()}.</p>
 *
 * <p>Note: on simulators, "Connect Hardware Keyboard" must be off or the software keyboard never
 * appears - CI disables it via simctl defaults (see scripts/boot-simulator.sh).</p>
 */
public class KeyboardComponent {

    private static final Locator DISMISS_KEY = Locator.named("keyboard.dismissKey")
            .classChain("**/XCUIElementTypeKeyboard/**/XCUIElementTypeButton[`name == 'Return' OR name == 'Done' OR name == 'Go' OR name == 'Search'`]")
            .predicate("type == 'XCUIElementTypeButton' AND (name == 'Done' OR name == 'Toolbar Done Button')")
            .build();

    private final IOSDriver driver;
    private final ElementFinder find;

    public KeyboardComponent(IOSDriver driver) {
        this.driver = driver;
        this.find = new ElementFinder(driver, "Keyboard");
    }

    public boolean isShown() {
        try {
            return driver.isKeyboardShown();
        } catch (RuntimeException e) {
            return false;
        }
    }

    public void waitUntilShown() {
        // Not every field raises a keyboard immediately (e.g. pickers); a short, non-failing wait.
        Waits.isTrueWithin(this::isShown, Waits.shortWait());
    }

    public void dismiss() {
        if (!isShown()) {
            return;
        }
        find.findNow(DISMISS_KEY).ifPresentOrElse(WebElement::click, driver::hideKeyboard);
        Waits.untilTrue(() -> !isShown(), Waits.shortWait(), "keyboard to be dismissed");
    }
}
