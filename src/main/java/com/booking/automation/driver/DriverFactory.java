package com.booking.automation.driver;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.support.ui.FluentWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates {@link IOSDriver} sessions.
 * <p>
 * Session creation is the single most common source of infrastructure flakiness on iOS
 * (WebDriverAgent build/launch, simulator boot). It is retried a bounded number of times using
 * {@link FluentWait} as the retry engine - no hand-written sleep loops - and every attempt is logged
 * so that infra flakiness is visible instead of silently absorbed.
 */
final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);

    private final FrameworkConfig config;
    private final IOSOptionsFactory optionsFactory;

    DriverFactory(FrameworkConfig config) {
        this.config = config;
        this.optionsFactory = new IOSOptionsFactory(config);
    }

    IOSDriver create(DeviceSpec device) {
        XCUITestOptions options = optionsFactory.build(device);
        int maxAttempts = Math.max(1, config.getInt(ConfigKeys.SESSION_CREATE_RETRIES, 2) + 1);
        AtomicInteger attempt = new AtomicInteger();

        return new FluentWait<>(options)
                .withTimeout(config.getSeconds(ConfigKeys.SESSION_WDA_LAUNCH_TIMEOUT, 120).multipliedBy(maxAttempts))
                .pollingEvery(Duration.ofSeconds(5))
                .ignoring(SessionNotCreatedException.class)
                .withMessage(() -> "Could not create an iOS session on " + device + " after " + attempt.get() + " attempt(s)")
                .until(opts -> {
                    int current = attempt.incrementAndGet();
                    if (current > maxAttempts) {
                        throw new WebDriverException("Session creation exhausted " + maxAttempts + " attempts on " + device);
                    }
                    LOG.info("Creating iOS session on {} (attempt {}/{})", device, current, maxAttempts);
                    IOSDriver driver = new IOSDriver(AppiumServerManager.serverUrl(), opts);
                    LOG.info("Session {} created on {}", driver.getSessionId(), device);
                    return driver;
                });
    }
}
