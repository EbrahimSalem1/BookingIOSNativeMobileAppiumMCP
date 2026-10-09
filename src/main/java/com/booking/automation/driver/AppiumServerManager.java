package com.booking.automation.driver;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import com.booking.automation.constants.FrameworkConstants;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Owns the Appium server process for the whole run.
 * <ul>
 *     <li>{@code appium.startLocal=true}: starts a server once per JVM and stops it on shutdown.</li>
 *     <li>{@code appium.startLocal=false}: connects to {@code appium.url} (CI service, device farm,
 *     or the same server the Appium MCP agent is attached to).</li>
 * </ul>
 */
public final class AppiumServerManager {

    private static final Logger LOG = LoggerFactory.getLogger(AppiumServerManager.class);
    private static AppiumDriverLocalService service;

    private AppiumServerManager() {
    }

    public static synchronized void startIfConfigured() {
        FrameworkConfig config = FrameworkConfig.get();
        if (!config.getBoolean(ConfigKeys.APPIUM_START_LOCAL, false) || isRunning()) {
            return;
        }
        service = new AppiumServiceBuilder()
                .usingPort(config.getInt(ConfigKeys.APPIUM_PORT, 4723))
                .withArgument(GeneralServerFlag.SESSION_OVERRIDE)
                .withArgument(GeneralServerFlag.LOG_LEVEL, config.getString(ConfigKeys.APPIUM_LOG_LEVEL, "info"))
                .withLogFile(FrameworkConstants.OUTPUT_DIR.resolve("appium-server.log").toFile())
                .build();
        service.start();
        Runtime.getRuntime().addShutdownHook(new Thread(AppiumServerManager::stop, "appium-shutdown"));
        LOG.info("Local Appium server started at {}", service.getUrl());
    }

    public static synchronized void stop() {
        if (isRunning()) {
            service.stop();
            LOG.info("Local Appium server stopped");
        }
    }

    public static synchronized URL serverUrl() {
        if (isRunning()) {
            return service.getUrl();
        }
        String url = FrameworkConfig.get().getString(ConfigKeys.APPIUM_URL);
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid appium.url: " + url, e);
        }
    }

    private static boolean isRunning() {
        return service != null && service.isRunning();
    }
}
