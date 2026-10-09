# Project rules for AI agents

This file is loaded automatically by Claude Code (and is plain Markdown for any other agent).
It is the contract that makes AI-generated code reviewable and mergeable. The same rules are
**enforced** by `src/test/java/com/booking/automation/unit/CodeConventionsTest.java` - code that
breaks them fails `mvn test -Punit`.

## Architecture you must follow
- Layers: `feature (business language) -> step definition (thin, asserts) -> page object (intent, no asserts) -> ElementFinder/Waits -> Appium`.
- Page objects live in `src/main/java/com/booking/automation/pages`, extend `BasePage`, declare a `screenAnchor()`,
  return the next page from navigation methods, and **never assert**.
- Shared preconditions go in `context/BookingJourney`, never step-calls-step.
- Per-scenario state goes in `context/ScenarioContext` (injected by PicoContainer). No static mutable state in tests.
- Test data goes in `src/test/resources/testdata/*.json` and is referenced by alias. No literals in steps or pages.

## Locators
- Declare locators with `Locator.named("screen.element")` and list strategies in this order:
  `accessibilityId` -> `predicate` / `classChain` -> `xpathAsLastResort` (justify any XPath in the PR).
- Only use locators you have **verified on the device** via the Appium MCP server (`appium-mcp`), never invented ones.
  Paste the evidence (page source excerpt or MCP output) in the PR description.

## Forbidden
- `Thread.sleep`, implicit waits, `DesiredCapabilities`, `TouchAction`, `MobileElement`, `MobileBy`.
- Assertions in page objects; UI words (click, tap, button, xpath, element, scroll) in `.feature` files.
- Hard-coded credentials or environment URLs anywhere. Secrets are `${ENV_VAR}` placeholders.

## Definition of done for generated code
1. `mvn -q test -Punit` is green.
2. New scenarios run green on a simulator at least 3 times in a row (`-Dcucumber.filter.tags=@new`).
3. Allure report shows meaningful step names and attachments on failure.
4. A human has reviewed assertions: they verify **business rules**, not that a tap happened.
