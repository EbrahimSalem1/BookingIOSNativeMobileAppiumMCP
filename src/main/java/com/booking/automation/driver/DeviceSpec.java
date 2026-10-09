package com.booking.automation.driver;

import java.util.Objects;
import java.util.Optional;

/**
 * One iOS target (simulator or real device) leased by one test thread.
 *
 * @param name            simulator/device name, e.g. "iPhone 15"
 * @param udid            optional UDID; mandatory for real devices and for parallel simulators
 * @param platformVersion iOS version, e.g. "17.5"
 * @param wdaLocalPort    WebDriverAgent port; must be unique per parallel session
 */
public record DeviceSpec(String name, String udid, String platformVersion, int wdaLocalPort) {

    public DeviceSpec {
        Objects.requireNonNull(name, "device name");
        Objects.requireNonNull(platformVersion, "platformVersion");
    }

    /**
     * Parses a pool entry. Supported forms: {@code "iPhone 15"}, {@code "iPhone 15@<udid>"},
     * {@code "iPhone 15@<udid>@17.5"} (per-device iOS version overrides the default).
     */
    public static DeviceSpec parse(String entry, String defaultPlatformVersion, int wdaLocalPort) {
        String[] parts = entry.trim().split("@");
        String name = parts[0].trim();
        String udid = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : null;
        String version = parts.length > 2 && !parts[2].isBlank() ? parts[2].trim() : defaultPlatformVersion;
        return new DeviceSpec(name, udid, version, wdaLocalPort);
    }

    public Optional<String> udidIfPresent() {
        return Optional.ofNullable(udid);
    }

    @Override
    public String toString() {
        return name + " (iOS " + platformVersion + (udid == null ? "" : ", " + udid) + ", wda:" + wdaLocalPort + ")";
    }
}
