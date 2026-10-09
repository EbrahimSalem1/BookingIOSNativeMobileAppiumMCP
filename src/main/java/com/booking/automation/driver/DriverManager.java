package com.booking.automation.driver;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import io.appium.java_client.ios.IOSDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-confined driver lifecycle. Each test thread owns one device lease and at most one session.
 *
 * <p>Two lifecycles are supported (config {@code session.lifecycle}):</p>
 * <ul>
 *     <li>{@link SessionLifecycle#PER_SCENARIO} - new session for every scenario. Maximum isolation,
 *     slower (WDA start ~10-30s). Default for CI release gates.</li>
 *     <li>{@link SessionLifecycle#PER_THREAD} - one session per thread; the app is terminated and
 *     relaunched between scenarios. Much faster for local runs and large suites.</li>
 * </ul>
 */
public final class DriverManager {

    private static final Logger LOG = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<IOSDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<DeviceSpec> DEVICE = new ThreadLocal<>();
    /** Every live session across threads, so PER_THREAD sessions can be closed at the end of the run. */
    private static final Set<IOSDriver> LIVE_SESSIONS = ConcurrentHashMap.newKeySet();

    private DriverManager() {
    }

    /** Called before each scenario: guarantees a live session with the app in the foreground at launch state. */
    public static void prepareSession() {
        FrameworkConfig config = FrameworkConfig.get();
        SessionLifecycle lifecycle = lifecycle();

        if (DRIVER.get() != null && lifecycle == SessionLifecycle.PER_THREAD) {
            relaunchApp(config);
            return;
        }
        if (DEVICE.get() == null) {
            DEVICE.set(DevicePool.get().lease());
        }
        IOSDriver driver = new DriverFactory(config).create(DEVICE.get());
        LIVE_SESSIONS.add(driver);
        DRIVER.set(driver);
    }

    /** Called after each scenario. In PER_THREAD mode the session survives until {@link #shutdown()}. */
    public static void finishScenario() {
        if (lifecycle() == SessionLifecycle.PER_SCENARIO) {
            shutdown();
        }
    }

    /** Quits the session and returns the device to the pool. Safe to call repeatedly. */
    public static void shutdown() {
        IOSDriver driver = DRIVER.get();
        try {
            if (driver != null) {
                LIVE_SESSIONS.remove(driver);
                driver.quit();
                LOG.info("Session {} closed", driver.getSessionId());
            }
        } catch (RuntimeException e) {
            // A dead session must never mask the real test result.
            LOG.warn("Ignoring error while quitting session: {}", e.getMessage());
        } finally {
            DRIVER.remove();
            DevicePool.get().release(DEVICE.get());
            DEVICE.remove();
        }
    }

    /** End-of-run safety net: quits sessions still open on any thread (PER_THREAD lifecycle). */
    public static void shutdownAll() {
        LIVE_SESSIONS.forEach(driver -> {
            try {
                driver.quit();
            } catch (RuntimeException e) {
                LOG.warn("Ignoring error while quitting session {}: {}", driver.getSessionId(), e.getMessage());
            }
        });
        LIVE_SESSIONS.clear();
    }

    public static IOSDriver driver() {
        IOSDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("No active iOS session on thread '" + Thread.currentThread().getName()
                    + "'. Is the @Before hook running?");
        }
        return driver;
    }

    public static Optional<IOSDriver> currentDriver() {
        return Optional.ofNullable(DRIVER.get());
    }

    public static Optional<DeviceSpec> currentDevice() {
        return Optional.ofNullable(DEVICE.get());
    }

    private static void relaunchApp(FrameworkConfig config) {
        String bundleId = config.getString(ConfigKeys.APP_BUNDLE_ID);
        IOSDriver driver = DRIVER.get();
        driver.terminateApp(bundleId);
        driver.activateApp(bundleId);
        LOG.debug("Relaunched {} on existing session {}", bundleId, driver.getSessionId());
    }

    private static SessionLifecycle lifecycle() {
        return FrameworkConfig.get().getEnum(ConfigKeys.SESSION_LIFECYCLE, SessionLifecycle.class,
                SessionLifecycle.PER_SCENARIO);
    }

    public enum SessionLifecycle {
        PER_SCENARIO, PER_THREAD
    }
}
