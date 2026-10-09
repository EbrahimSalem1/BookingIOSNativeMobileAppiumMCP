package com.booking.automation.hooks;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import com.booking.automation.driver.DriverManager;
import com.booking.automation.utils.Evidence;
import com.booking.automation.utils.Evidence.Mode;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Scenario lifecycle: session up before, evidence on failure, session down after.
 * Ordering matters: evidence must be captured (order 100) before the session is closed (order 0),
 * because Cucumber runs {@code @After} hooks in descending order.
 */
public class ScenarioHooks {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioHooks.class);

    private final Mode screenshotMode;
    private final Mode videoMode;
    private final boolean pageSourceOnFailure;

    public ScenarioHooks() {
        FrameworkConfig config = FrameworkConfig.get();
        this.screenshotMode = config.getEnum(ConfigKeys.EVIDENCE_SCREENSHOT, Mode.class, Mode.ON_FAILURE);
        this.videoMode = config.getEnum(ConfigKeys.EVIDENCE_VIDEO, Mode.class, Mode.NEVER);
        this.pageSourceOnFailure = config.getBoolean(ConfigKeys.EVIDENCE_PAGE_SOURCE_ON_FAILURE, true);
    }

    @Before(order = 0)
    public void startSession(Scenario scenario) {
        MDC.put("scenario", scenario.getName());     // every log line carries the scenario name
        LOG.info(">> Scenario: {} {}", scenario.getName(), scenario.getSourceTagNames());
        DriverManager.prepareSession();
        Allure.parameter("Device", Evidence.deviceSummary());
        if (videoMode != Mode.NEVER) {
            Evidence.startVideo();
        }
    }

    @AfterStep
    public void afterStep(Scenario scenario) {
        if (screenshotMode == Mode.EACH_STEP) {
            Evidence.screenshot("Step screenshot");
        }
    }

    @After(order = 100)
    public void collectEvidence(Scenario scenario) {
        boolean failed = scenario.isFailed();
        if (failed) {
            LOG.error("!! Scenario failed: {} on {}", scenario.getName(), Evidence.deviceSummary());
            if (screenshotMode != Mode.NEVER) {
                Evidence.screenshot("Failure screenshot");
            }
            if (pageSourceOnFailure) {
                Evidence.pageSource("Failure page source (XCUITest tree)");
            }
        }
        if (videoMode != Mode.NEVER) {
            Evidence.stopVideo(failed);
        }
    }

    @After(order = 0)
    public void endSession(Scenario scenario) {
        try {
            DriverManager.finishScenario();
        } finally {
            LOG.info("<< Scenario finished: {} [{}]", scenario.getName(), scenario.getStatus());
            MDC.remove("scenario");
        }
    }
}
