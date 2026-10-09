package com.booking.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Re-runs only the scenarios that failed in the main pass ({@code mvn test -Prerun}).
 *
 * <p>Flaky-test policy: exactly <b>one</b> rerun, in a fresh session. A scenario that fails then
 * passes is reported as <em>flaky</em> in Allure (retries tab) and tracked - it is not silently
 * green. A rerun is a diagnostic tool, not a way to hide instability.</p>
 */
@CucumberOptions(
        features = "@target/rerun/failed.txt",
        glue = {
                "com.booking.automation.stepdefinitions",
                "com.booking.automation.hooks"
        },
        plugin = {
                "pretty",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "json:target/cucumber/cucumber-rerun.json",
                "rerun:target/rerun/failed-after-rerun.txt"
        },
        monochrome = true
)
public class RerunFailedRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
