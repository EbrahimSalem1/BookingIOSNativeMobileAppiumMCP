package com.booking.automation.utils;

import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Mobile gestures. Prefers XCUITest's native {@code mobile:} commands (executed inside WDA,
 * stable across iOS versions) and falls back to W3C Actions only where coordinates are required.
 * The deprecated TouchAction API is intentionally not used.
 */
public final class Gestures {

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    private final IOSDriver driver;

    public Gestures(IOSDriver driver) {
        this.driver = driver;
    }

    /** One native scroll of roughly one screen inside {@code container} (or the whole app if null). */
    public void scroll(Direction direction, WebElement container) {
        Map<String, Object> args = container == null
                ? Map.of("direction", direction.name().toLowerCase())
                : Map.of("direction", direction.name().toLowerCase(), "elementId", ((RemoteWebElement) container).getId());
        driver.executeScript("mobile: scroll", args);
    }

    /** Native swipe; useful for carousels (e.g. the image gallery on the details screen). */
    public void swipe(Direction direction, WebElement element) {
        driver.executeScript("mobile: swipe", Map.of(
                "direction", direction.name().toLowerCase(),
                "elementId", ((RemoteWebElement) element).getId()));
    }

    /** Tap via W3C pointer actions, for elements XCUITest reports as not hittable (e.g. overlays). */
    public void tapAtCentre(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        int x = location.getX() + size.getWidth() / 2;
        int y = location.getY() + size.getHeight() / 2;

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(tap));
    }
}
