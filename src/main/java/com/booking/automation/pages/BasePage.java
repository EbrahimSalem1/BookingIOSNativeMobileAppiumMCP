package com.booking.automation.pages;

import com.booking.automation.components.KeyboardComponent;
import com.booking.automation.components.LoadingIndicatorComponent;
import com.booking.automation.driver.DriverManager;
import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.utils.Gestures;
import com.booking.automation.utils.Waits;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Base for all screens.
 *
 * <p>Design rules for page objects in this framework:</p>
 * <ul>
 *     <li>Pages expose <b>user intentions</b> ({@code loginAs}, {@code applyFilter}), never raw clicks.</li>
 *     <li>Pages <b>never assert</b>. They return state (models, booleans, text); step definitions decide
 *     what is correct. This keeps pages reusable across positive and negative scenarios.</li>
 *     <li>Navigation methods return the next page object (fluent, type-safe journeys).</li>
 *     <li>Every page declares a {@link #screenAnchor()} - one element that proves the screen is shown.</li>
 * </ul>
 */
public abstract class BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final IOSDriver driver;
    protected final ElementFinder find;
    protected final Gestures gestures;
    protected final KeyboardComponent keyboard;
    protected final LoadingIndicatorComponent loading;

    protected BasePage() {
        this.driver = DriverManager.driver();
        this.find = new ElementFinder(driver, getClass().getSimpleName());
        this.gestures = new Gestures(driver);
        this.keyboard = new KeyboardComponent(driver);
        this.loading = new LoadingIndicatorComponent(driver);
    }

    /** An element that uniquely identifies this screen. */
    protected abstract Locator screenAnchor();

    /** True when the screen is shown within the standard explicit wait. */
    public boolean isDisplayed() {
        return isVisible(screenAnchor(), Waits.explicit());
    }

    /** Fails fast with a precise message if the screen did not appear. Returns {@code this} for chaining. */
    @SuppressWarnings("unchecked")
    protected <T extends BasePage> T waitUntilLoaded() {
        loading.waitUntilGone(Waits.longWait());
        find.visible(screenAnchor(), Waits.explicit());
        log.debug("{} is displayed", getClass().getSimpleName());
        return (T) this;
    }

    // ------------------------------------------------------------- interaction primitives

    protected void tap(Locator locator) {
        WebElement element = find.visible(locator, Waits.explicit());
        Waits.untilTrue(element::isEnabled, Waits.shortWait(), "'" + locator.name() + "' to be enabled");
        element.click();
    }

    protected void type(Locator locator, String text) {
        WebElement field = find.visible(locator, Waits.explicit());
        field.click();                       // focus first so the keyboard is up before typing
        keyboard.waitUntilShown();
        field.clear();
        field.sendKeys(text);
        log.debug("Typed into '{}'", locator.name());
    }

    protected String textOf(Locator locator) {
        return ElementFinder.readText(find.visible(locator, Waits.explicit()));
    }

    protected boolean isVisible(Locator locator, Duration within) {
        return Waits.isTrueWithin(() -> find.findNow(locator).map(WebElement::isDisplayed).orElse(false), within);
    }

    protected boolean isEnabled(Locator locator) {
        return find.findNow(locator).map(WebElement::isEnabled).orElse(false);
    }
}
