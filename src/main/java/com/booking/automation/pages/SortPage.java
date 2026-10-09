package com.booking.automation.pages;

import com.booking.automation.locators.Locator;
import com.booking.automation.utils.Waits;
import io.qameta.allure.Step;

/** Sort action sheet / screen. */
public class SortPage extends BasePage {

    private static final Locator SHEET = Locator.named("sort.sheet")
            .accessibilityId("sort_screen")
            .classChain("**/XCUIElementTypeSheet")
            .predicate("type == 'XCUIElementTypeNavigationBar' AND name CONTAINS[c] 'Sort'")
            .build();
    private static final Locator APPLY = Locator.named("sort.apply")
            .accessibilityId("sort_apply_button")
            .predicate("type == 'XCUIElementTypeButton' AND label BEGINSWITH[c] 'Apply'")
            .build();

    @Override
    protected Locator screenAnchor() {
        return SHEET;
    }

    /**
     * Chooses an option by its on-screen label. Some apps apply immediately on tap (action sheet),
     * others need "Apply" - both are handled.
     */
    @Step("Sort by '{uiLabel}'")
    public SearchResultsPage sortBy(String uiLabel) {
        tap(Locator.byLabel("sort.option", uiLabel));
        if (isVisible(APPLY, Waits.shortWait())) {
            tap(APPLY);
        }
        return new SearchResultsPage().waitForResults();
    }
}
