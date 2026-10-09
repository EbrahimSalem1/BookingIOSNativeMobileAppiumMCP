package com.booking.automation.constants;

/** Every configuration key in one place: no magic strings scattered across the framework. */
public final class ConfigKeys {

    // Appium server
    public static final String APPIUM_URL = "appium.url";
    public static final String APPIUM_START_LOCAL = "appium.startLocal";
    public static final String APPIUM_PORT = "appium.port";
    public static final String APPIUM_LOG_LEVEL = "appium.logLevel";

    // Application under test
    public static final String APP_PATH = "app.path";
    public static final String APP_BUNDLE_ID = "app.bundleId";

    // Devices - comma separated pool; each parallel thread leases one
    public static final String DEVICE_POOL = "device.pool";
    public static final String DEVICE_PLATFORM_VERSION = "device.platformVersion";
    public static final String DEVICE_IS_REAL = "device.real";
    public static final String DEVICE_WDA_BASE_PORT = "device.wdaBasePort";
    public static final String DEVICE_XCODE_ORG_ID = "device.xcodeOrgId";
    public static final String DEVICE_XCODE_SIGNING_ID = "device.xcodeSigningId";

    // Session behaviour
    public static final String SESSION_LIFECYCLE = "session.lifecycle";
    public static final String SESSION_NO_RESET = "session.noReset";
    public static final String SESSION_NEW_COMMAND_TIMEOUT = "session.newCommandTimeoutSeconds";
    public static final String SESSION_WDA_LAUNCH_TIMEOUT = "session.wdaLaunchTimeoutSeconds";
    public static final String SESSION_CREATE_RETRIES = "session.createRetries";

    // Synchronisation
    public static final String WAIT_EXPLICIT = "wait.explicitSeconds";
    public static final String WAIT_SHORT = "wait.shortSeconds";
    public static final String WAIT_LONG = "wait.longSeconds";
    public static final String WAIT_POLLING = "wait.pollingMillis";

    // Evidence
    public static final String EVIDENCE_SCREENSHOT = "evidence.screenshot";
    public static final String EVIDENCE_PAGE_SOURCE_ON_FAILURE = "evidence.pageSourceOnFailure";
    public static final String EVIDENCE_VIDEO = "evidence.video";

    // Capability pass-through: anything under this prefix is added verbatim to XCUITestOptions
    public static final String EXTRA_CAPABILITIES_PREFIX = "capabilities.";

    // Search form behaviour (apps differ: some prefill dates/guests, some require them)
    public static final String SEARCH_SET_DATES = "search.setDates";
    public static final String SEARCH_SET_GUESTS = "search.setGuests";
    public static final String SEARCH_CALENDAR_DAY_PATTERN = "search.calendarDayLabelPattern";

    // Results-list harvesting
    public static final String RESULTS_SAMPLE_SIZE = "results.sampleSize";
    public static final String RESULTS_MAX_SCROLLS = "results.maxScrolls";

    private ConfigKeys() {
    }
}
