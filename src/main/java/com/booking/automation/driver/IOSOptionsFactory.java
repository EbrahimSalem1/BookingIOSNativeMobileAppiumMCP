package com.booking.automation.driver;

import com.booking.automation.config.ConfigurationException;
import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Builds W3C-compliant {@link XCUITestOptions} (no legacy DesiredCapabilities).
 * <p>
 * Everything is data driven: the common capabilities are typed setters, and anything else
 * the team needs (e.g. {@code appium:processArguments}, {@code appium:reduceMotion}) can be
 * added in YAML under {@code capabilities.*} without touching Java.
 */
public final class IOSOptionsFactory {

    private final FrameworkConfig config;

    public IOSOptionsFactory(FrameworkConfig config) {
        this.config = config;
    }

    public XCUITestOptions build(DeviceSpec device) {
        XCUITestOptions options = new XCUITestOptions()     // sets platformName=iOS, automationName=XCUITest
                .setDeviceName(device.name())
                .setPlatformVersion(device.platformVersion())
                .setWdaLocalPort(device.wdaLocalPort())
                .setNoReset(config.getBoolean(ConfigKeys.SESSION_NO_RESET, false))
                .setNewCommandTimeout(config.getSeconds(ConfigKeys.SESSION_NEW_COMMAND_TIMEOUT, 120))
                .setWdaLaunchTimeout(config.getSeconds(ConfigKeys.SESSION_WDA_LAUNCH_TIMEOUT, 120));

        device.udidIfPresent().ifPresent(options::setUdid);
        applyApplication(options);
        applyRealDeviceSigning(options);

        // Pass-through capabilities from YAML. Keys are used verbatim, so vendor prefixes are explicit.
        CapabilityMapper.toCapabilities(config.subset(ConfigKeys.EXTRA_CAPABILITIES_PREFIX)).forEach(options::amend);
        return options;
    }

    /**
     * Either install a build ({@code app.path}: .app for simulators, .ipa for devices) or attach to
     * an already-installed app by bundle id. Installing is preferred in CI for build traceability.
     */
    private void applyApplication(XCUITestOptions options) {
        Optional<String> appPath = config.optionalString(ConfigKeys.APP_PATH);
        Optional<String> bundleId = config.optionalString(ConfigKeys.APP_BUNDLE_ID);

        if (appPath.isPresent()) {
            String path = appPath.get();
            boolean isRemote = path.startsWith("http") || path.startsWith("bs://") || path.startsWith("storage:");
            if (!isRemote && !Files.exists(Path.of(path))) {
                throw new ConfigurationException("app.path does not exist: " + Path.of(path).toAbsolutePath());
            }
            options.setApp(path);
        }
        bundleId.ifPresent(options::setBundleId);

        if (appPath.isEmpty() && bundleId.isEmpty()) {
            throw new ConfigurationException("Configure either 'app.path' or 'app.bundleId'");
        }
    }

    private void applyRealDeviceSigning(XCUITestOptions options) {
        if (!config.getBoolean(ConfigKeys.DEVICE_IS_REAL, false)) {
            return;
        }
        // Self-hosted real devices need WebDriverAgent signed with the team's provisioning profile.
        // Device clouds sign WDA themselves, so the team id is optional there.
        config.optionalString(ConfigKeys.DEVICE_XCODE_ORG_ID).ifPresent(teamId -> {
            options.setXcodeOrgId(teamId);
            options.setXcodeSigningId(config.getString(ConfigKeys.DEVICE_XCODE_SIGNING_ID, "Apple Development"));
        });
    }
}
