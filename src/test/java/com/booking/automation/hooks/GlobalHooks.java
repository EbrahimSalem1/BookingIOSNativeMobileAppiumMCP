package com.booking.automation.hooks;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.FrameworkConstants;
import com.booking.automation.driver.AppiumServerManager;
import com.booking.automation.driver.DriverManager;
import com.booking.automation.locators.LocatorDriftTracker;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.Set;

/** Once-per-run setup and teardown. */
public final class GlobalHooks {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalHooks.class);
    private static final Set<String> ENVIRONMENT_KEYS = Set.of(
            "device.pool", "device.platformVersion", "device.real", "app.bundleId", "app.path",
            "appium.url", "session.lifecycle");

    @BeforeAll
    public static void beforeRun() {
        FrameworkConfig config = FrameworkConfig.get();   // fail fast on bad configuration
        AppiumServerManager.startIfConfigured();
        writeAllureEnvironment(config);
        copyAllureCategories();
    }

    @AfterAll
    public static void afterRun() {
        DriverManager.shutdownAll();
        LocatorDriftTracker.writeReport();
        AppiumServerManager.stop();
    }

    /** Failure categories (locator / product bug / sync / infra / data) for Allure's Categories tab. */
    private static void copyAllureCategories() {
        try (InputStream in = GlobalHooks.class.getClassLoader().getResourceAsStream("categories.json")) {
            if (in != null) {
                Files.createDirectories(FrameworkConstants.ALLURE_RESULTS_DIR);
                Files.copy(in, FrameworkConstants.ALLURE_RESULTS_DIR.resolve("categories.json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOG.warn("Could not copy Allure categories: {}", e.getMessage());
        }
    }

    /** Populates the Allure "Environment" widget so every report states exactly what was tested. */
    private static void writeAllureEnvironment(FrameworkConfig config) {
        Properties properties = new Properties();
        properties.setProperty("Environment", config.environment());
        config.safeSnapshot().forEach((key, value) -> {
            if (ENVIRONMENT_KEYS.contains(key)) {
                properties.setProperty(key, value);
            }
        });
        properties.setProperty("Java", System.getProperty("java.version"));
        properties.setProperty("Tags", System.getProperty("cucumber.filter.tags", "(all)"));

        Path file = FrameworkConstants.ALLURE_RESULTS_DIR.resolve("environment.properties");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                properties.store(writer, "Allure environment");
            }
        } catch (IOException e) {
            LOG.warn("Could not write Allure environment file: {}", e.getMessage());
        }
    }
}
