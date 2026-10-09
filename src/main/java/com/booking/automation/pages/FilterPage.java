package com.booking.automation.pages;

import com.booking.automation.locators.Locator;
import com.booking.automation.models.FilterCriteria;
import com.booking.automation.utils.Gestures;
import com.booking.automation.utils.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/** Filter sheet / screen. Options are located by the label from test data, so new filters need no code. */
public class FilterPage extends BasePage {

    private static final Locator SHEET = Locator.named("filter.sheet")
            .accessibilityId("filter_screen")
            .predicate("type == 'XCUIElementTypeNavigationBar' AND name CONTAINS[c] 'Filter'")
            .build();
    private static final Locator SCROLL_CONTAINER = Locator.named("filter.scrollContainer")
            .accessibilityId("filter_options_list")
            .classChain("**/XCUIElementTypeScrollView")
            .classChain("**/XCUIElementTypeTable")
            .build();
    private static final Locator APPLY = Locator.named("filter.apply")
            .accessibilityId("filter_apply_button")
            .predicate("type == 'XCUIElementTypeButton' AND (label BEGINSWITH[c] 'Apply' OR label BEGINSWITH[c] 'Show')")
            .build();
    private static final Locator RESET = Locator.named("filter.reset")
            .accessibilityId("filter_reset_button")
            .predicate("type == 'XCUIElementTypeButton' AND (label ==[c] 'Reset' OR label ==[c] 'Clear all')")
            .build();

    private static final int MAX_SCROLLS = 5;

    @Override
    protected Locator screenAnchor() {
        return SHEET;
    }

    @Step("Select filter {criteria.uiLabel}")
    public FilterPage select(FilterCriteria criteria) {
        Locator option = Locator.byLabel("filter.option", criteria.uiLabel());
        WebElement container = find.visible(SCROLL_CONTAINER, Waits.explicit());
        for (int i = 0; i <= MAX_SCROLLS; i++) {
            var element = find.findNow(option).filter(WebElement::isDisplayed);
            if (element.isPresent()) {
                element.get().click();
                return this;
            }
            gestures.scroll(Gestures.Direction.DOWN, container);
        }
        throw new IllegalStateException("Filter option '" + criteria.uiLabel() + "' not found on the filter screen");
    }

    /** "Show 42 results" style buttons are useful: they predict the result count before applying. */
    public String applyButtonLabel() {
        return textOf(APPLY);
    }

    @Step("Apply filters")
    public SearchResultsPage apply() {
        tap(APPLY);
        return new SearchResultsPage().waitForResults();
    }

    public FilterPage reset() {
        tap(RESET);
        return this;
    }
}
