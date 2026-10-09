package com.booking.automation.driver;

import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Device-free tests for device-pool parsing and capability mapping (package-private API). */
public class DriverSetupTest {

    @Test
    public void parsesDevicePoolEntries() {
        DeviceSpec simple = DeviceSpec.parse("iPhone 15", "17.5", 8100);
        assertThat(simple.udid()).isNull();
        assertThat(simple.platformVersion()).isEqualTo("17.5");

        DeviceSpec withUdid = DeviceSpec.parse(" iPhone 15 Pro@ABC-123 ", "17.5", 8101);
        assertThat(withUdid.name()).isEqualTo("iPhone 15 Pro");
        assertThat(withUdid.udid()).isEqualTo("ABC-123");

        DeviceSpec cloud = DeviceSpec.parse("iPhone 13@@16", "17.5", 8102);
        assertThat(cloud.udid()).isNull();
        assertThat(cloud.platformVersion()).isEqualTo("16");
    }

    @Test
    public void mapsFlatConfigToTypedNestedCapabilities() {
        Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("appium:reduceMotion", "true");
        flat.put("appium:derivedDataPath", "");
        flat.put("appium:wdaStartupRetries", "3");
        flat.put("bstack:options.userName", "qa-bot");
        flat.put("bstack:options.projectName", "Booking iOS");

        Map<String, Object> caps = CapabilityMapper.toCapabilities(flat);

        assertThat(caps).containsEntry("appium:reduceMotion", true)
                .containsEntry("appium:wdaStartupRetries", 3)
                .doesNotContainKey("appium:derivedDataPath");
        assertThat(caps.get("bstack:options")).isEqualTo(Map.of("userName", "qa-bot", "projectName", "Booking iOS"));
    }
}
