package com.booking.automation.unit;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Executable coding standards.
 *
 * <p>These rules are what make AI-generated code safe to merge: an agent can produce a page object
 * in seconds, but it cannot merge one that sleeps, uses legacy APIs or leaks UI details into
 * Gherkin. The same rules are written in {@code CLAUDE.md} so the agent follows them up front;
 * this test makes them non-negotiable.</p>
 */
public class CodeConventionsTest {

    private static final Path MAIN = Path.of("src/main/java");
    private static final Path TEST = Path.of("src/test/java");
    private static final Path FEATURES = Path.of("src/test/resources/features");

    @Test
    public void noHardSleeps() {
        assertNoMatch(javaSources(), Pattern.compile("Thread\\.sleep\\s*\\("), "Thread.sleep - use Waits instead");
    }

    @Test
    public void noLegacyAppiumApis() {
        assertNoMatch(javaSources(), Pattern.compile("\\b(DesiredCapabilities|TouchAction|MobileElement|MobileBy)\\b"),
                "legacy Appium/Selenium API - use XCUITestOptions, W3C actions, AppiumBy");
    }

    @Test
    public void noImplicitWaits() {
        assertNoMatch(javaSources(), Pattern.compile("implicitlyWait\\s*\\("), "implicit wait - explicit waits only");
    }

    @Test
    public void pageObjectsDoNotAssert() {
        assertNoMatch(files(MAIN.resolve("com/booking/automation/pages"), ".java"),
                Pattern.compile("\\b(assertThat|Assert\\.)"), "assertion inside a page object - assert in steps");
    }

    @Test
    public void noHardCodedCredentialsInCode() {
        assertNoMatch(javaSources(), Pattern.compile("(?i)(password|passwd)\\s*=\\s*\"[^\"$]+\""), "hard-coded password");
    }

    @Test
    public void featureFilesDescribeBehaviourNotImplementation() {
        assertNoMatch(files(FEATURES, ".feature"),
                Pattern.compile("(?i)\\b(click|tap|xpath|accessibility id|button with|locator|element|swipe|scroll)\\b"),
                "implementation detail in Gherkin");
    }

    @Test
    public void xpathIsTheExceptionNotTheRule() {
        long xpathLocators = count(files(MAIN, ".java"), Pattern.compile("xpathAsLastResort\\("));
        long allStrategies = count(files(MAIN, ".java"), Pattern.compile("\\.(accessibilityId|predicate|classChain|xpathAsLastResort)\\("));
        assertThat(xpathLocators).as("XPath strategies (%d of %d)", xpathLocators, allStrategies)
                .isLessThanOrEqualTo(Math.max(1, allStrategies / 10));
    }

    // ------------------------------------------------------------------ helpers

    private static List<Path> javaSources() {
        return Stream.concat(files(MAIN, ".java").stream(), files(TEST, ".java").stream())
                .filter(p -> !p.endsWith("CodeConventionsTest.java"))
                .toList();
    }

    private static List<Path> files(Path root, String extension) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(p -> p.toString().endsWith(extension)).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void assertNoMatch(List<Path> files, Pattern forbidden, String rule) {
        List<String> violations = files.stream()
                .flatMap(file -> lines(file).stream()
                        .filter(line -> !line.trim().startsWith("*") && !line.trim().startsWith("//"))
                        .filter(line -> forbidden.matcher(line).find())
                        .map(line -> file + ": " + line.trim()))
                .toList();
        assertThat(violations).as("Rule violated: " + rule).isEmpty();
    }

    private static long count(List<Path> files, Pattern pattern) {
        return files.stream().flatMap(f -> lines(f).stream()).filter(l -> pattern.matcher(l).find()).count();
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
