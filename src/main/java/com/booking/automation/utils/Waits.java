package com.booking.automation.utils;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.FluentWait;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The only synchronisation primitive in the framework. {@code Thread.sleep} is banned (enforced by
 * {@code CodeConventionsTest}); every wait is a condition with a timeout and a descriptive message,
 * so a timeout explains <em>what</em> never happened instead of just "timed out".
 *
 * <p>Implicit waits stay at 0 for the whole session: mixing implicit and explicit waits multiplies
 * timeouts and makes fallback locator probing slow.</p>
 */
public final class Waits {

    private Waits() {
    }

    public static Duration explicit() {
        return FrameworkConfig.get().getSeconds(ConfigKeys.WAIT_EXPLICIT, 15);
    }

    public static Duration shortWait() {
        return FrameworkConfig.get().getSeconds(ConfigKeys.WAIT_SHORT, 3);
    }

    public static Duration longWait() {
        return FrameworkConfig.get().getSeconds(ConfigKeys.WAIT_LONG, 45);
    }

    private static Duration polling() {
        return FrameworkConfig.get().getMillis(ConfigKeys.WAIT_POLLING, 300);
    }

    /** Polls {@code condition} until it returns a non-null / non-false value. */
    public static <T> T until(Supplier<T> condition, Duration timeout, String description) {
        return new FluentWait<>(condition)
                .withTimeout(timeout)
                .pollingEvery(polling())
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .withMessage(() -> "Waited " + timeout.toSeconds() + "s for: " + description)
                .until(Supplier::get);
    }

    public static void untilTrue(BooleanSupplier condition, Duration timeout, String description) {
        until(() -> condition.getAsBoolean() ? Boolean.TRUE : null, timeout, description);
    }

    /** Non-throwing variant for "is X present within a short window" style checks. */
    public static boolean isTrueWithin(BooleanSupplier condition, Duration timeout) {
        try {
            untilTrue(condition, timeout, "condition");
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits until a value stops changing between two consecutive polls. Used after scrolls and
     * after filter/sort are applied, when a list re-renders asynchronously and there is no
     * spinner to wait for. Replaces the classic "sleep 2 seconds after scrolling".
     */
    public static <T> T untilStable(Supplier<T> sampler, Duration timeout, String description) {
        AtomicReference<T> previous = new AtomicReference<>();
        AtomicReference<Boolean> first = new AtomicReference<>(Boolean.TRUE);
        return until(() -> {
            T current = sampler.get();
            if (Boolean.TRUE.equals(first.getAndSet(Boolean.FALSE))) {
                previous.set(current);
                return null;
            }
            if (Objects.equals(previous.get(), current)) {
                return current;
            }
            previous.set(current);
            return null;
        }, timeout, description + " to become stable");
    }
}
