package com.booking.automation.utils;

import com.booking.automation.driver.DriverManager;
import io.appium.java_client.ios.IOSDriver;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Collects debugging evidence and attaches it to the Allure report. Every method is
 * failure-tolerant: evidence collection must never turn a test failure into a different error.
 */
public final class Evidence {

    private static final Logger LOG = LoggerFactory.getLogger(Evidence.class);

    /** When to capture screenshots / video (config: evidence.screenshot, evidence.video). */
    public enum Mode { ON_FAILURE, EACH_STEP, NEVER }

    private Evidence() {
    }

    public static void screenshot(String name) {
        DriverManager.currentDriver().ifPresent(driver -> safely("screenshot", () -> {
            byte[] png = driver.getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), "png");
        }));
    }

    /** The XCUITest element tree: the single most useful artefact for locator failures. */
    public static void pageSource(String name) {
        DriverManager.currentDriver().ifPresent(driver -> safely("page source", () ->
                Allure.addAttachment(name, "application/xml", driver.getPageSource(), "xml")));
    }

    public static void startVideo() {
        DriverManager.currentDriver().ifPresent(driver -> safely("start video", driver::startRecordingScreen));
    }

    /** Stops recording; attaches only when {@code attach} is true (typically on failure). */
    public static void stopVideo(boolean attach) {
        DriverManager.currentDriver().ifPresent(driver -> safely("stop video", () -> {
            String base64 = driver.stopRecordingScreen();
            if (attach && base64 != null && !base64.isBlank()) {
                Allure.addAttachment("Screen recording", "video/mp4",
                        new ByteArrayInputStream(Base64.getMimeDecoder().decode(base64)), "mp4");
            }
        }));
    }

    public static void text(String name, String content) {
        Allure.addAttachment(name, "text/plain", content, "txt");
    }

    public static void file(String name, Path path, String mimeType) {
        safely("file " + path, () -> {
            if (Files.exists(path)) {
                Allure.addAttachment(name, mimeType, Files.readString(path, StandardCharsets.UTF_8), extension(path));
            }
        });
    }

    public static String deviceSummary() {
        return DriverManager.currentDevice().map(Object::toString).orElse("no device")
                + DriverManager.currentDriver().map(IOSDriver::getSessionId).map(id -> " | session " + id).orElse("");
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "txt" : name.substring(dot + 1);
    }

    private static void safely(String what, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Exception e) {
            LOG.warn("Could not capture {}: {}", what, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
