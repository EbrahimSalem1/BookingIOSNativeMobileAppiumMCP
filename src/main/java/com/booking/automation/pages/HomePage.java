package com.booking.automation.pages;

import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import io.qameta.allure.Step;

import java.time.Duration;

/** Landing screen after a successful login. */
public class HomePage extends BasePage {

    private static final Locator HOME_CONTAINER = Locator.named("home.container")
            .accessibilityId("home_screen")
            .predicate("type == 'XCUIElementTypeTabBar'")
            .build();
    private static final Locator GREETING = Locator.named("home.greeting")
            .accessibilityId("home_greeting_label")
            .build();
    private static final Locator SEARCH_ENTRY = Locator.named("home.searchEntry")
            .accessibilityId("home_search_entry")
            .predicate("type == 'XCUIElementTypeButton' AND (label CONTAINS[c] 'search' OR name ==[c] 'Search')")
            .classChain("**/XCUIElementTypeTabBar/XCUIElementTypeButton[`label CONTAINS[c] 'Search'`]")
            .build();
    private static final Locator LOGOUT = Locator.named("home.logout")
            .accessibilityId("home_logout_button")
            .build();

    @Override
    protected Locator screenAnchor() {
        return HOME_CONTAINER;
    }

    /** Signals of an authenticated session: home is shown and the search entry point is reachable. */
    public boolean isUserLoggedIn() {
        return isDisplayed() && isVisible(SEARCH_ENTRY, Duration.ZERO);
    }

    public String greeting() {
        return find.findNow(GREETING).map(ElementFinder::readText).orElse("");
    }

    @Step("Open search")
    public SearchPage openSearch() {
        tap(SEARCH_ENTRY);
        return new SearchPage().waitUntilLoaded();
    }

    public boolean canLogOut() {
        return find.findNow(LOGOUT).isPresent();
    }
}
