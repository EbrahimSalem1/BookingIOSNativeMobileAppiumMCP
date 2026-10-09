package com.booking.automation.constants;

import java.nio.file.Path;

/** Framework-wide constants that are not environment specific (those live in YAML config). */
public final class FrameworkConstants {

    public static final String ENV_PROPERTY = "env";
    public static final String DEFAULT_ENV = "local";
    public static final String CONFIG_DIR = "config/";
    public static final String TEST_DATA_DIR = "testdata/";

    public static final Path OUTPUT_DIR = Path.of("target");
    public static final Path ALLURE_RESULTS_DIR = OUTPUT_DIR.resolve("allure-results");
    public static final Path EVIDENCE_DIR = OUTPUT_DIR.resolve("evidence");
    public static final Path LOCATOR_DRIFT_REPORT = OUTPUT_DIR.resolve("locator-drift-report.json");

    private FrameworkConstants() {
    }
}
