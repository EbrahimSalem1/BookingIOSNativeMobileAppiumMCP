package com.booking.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Main entry point. Tag selection is external so one runner serves every pipeline stage:
 * <pre>
 *   mvn test -Dcucumber.filter.tags="@smoke"                    # PR gate
 *   mvn test -Dcucumber.filter.tags="@regression and not @wip"  # nightly
 *   mvn test -Dthreads=3 -Denv=ci                               # parallel, 3 simulators
 * </pre>
 * Failed scenarios are written to {@code target/rerun/failed.txt} for {@link RerunFailedRunner}.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "com.booking.automation.stepdefinitions",
                "com.booking.automation.hooks"
        },
        plugin = {
                "pretty",
                "summary",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
                "json:target/cucumber/cucumber.json",
                "html:target/cucumber/cucumber.html",
                "rerun:target/rerun/failed.txt"
        },
        monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /** Scenarios run in parallel; thread count = -Dthreads (TestNG data-provider-thread-count). */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
