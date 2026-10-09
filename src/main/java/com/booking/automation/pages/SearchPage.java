package com.booking.automation.pages;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.models.SearchCriteria;
import com.booking.automation.utils.Gestures;
import com.booking.automation.utils.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class SearchPage extends BasePage {

    private static final Locator DESTINATION = Locator.named("search.destination")
            .accessibilityId("search_destination_field")
            .predicate("type IN {'XCUIElementTypeSearchField','XCUIElementTypeTextField'} AND placeholderValue CONTAINS[c] 'where'")
            .classChain("**/XCUIElementTypeSearchField")
            .build();
    private static final Locator FIRST_SUGGESTION = Locator.named("search.firstSuggestion")
            .accessibilityId("search_suggestion_0")
            .classChain("**/XCUIElementTypeTable/XCUIElementTypeCell[1]")
            .build();
    private static final Locator DATES = Locator.named("search.dates")
            .accessibilityId("search_dates_field")
            .build();
    private static final Locator CALENDAR = Locator.named("search.calendar")
            .accessibilityId("search_calendar")
            .classChain("**/XCUIElementTypeCollectionView")
            .build();
    private static final Locator CONFIRM_DATES = Locator.named("search.confirmDates")
            .accessibilityId("search_dates_confirm")
            .predicate("type == 'XCUIElementTypeButton' AND (label ==[c] 'Done' OR label ==[c] 'Select dates')")
            .build();
    private static final Locator ADULTS_VALUE = Locator.named("search.adultsValue")
            .accessibilityId("search_adults_value")
            .build();
    private static final Locator ADULTS_INCREMENT = Locator.named("search.adultsIncrement")
            .accessibilityId("search_adults_increment")
            .classChain("**/XCUIElementTypeStepper[`name == 'search_adults_stepper'`]/XCUIElementTypeButton[`name == 'Increment'`]")
            .build();
    private static final Locator ADULTS_DECREMENT = Locator.named("search.adultsDecrement")
            .accessibilityId("search_adults_decrement")
            .classChain("**/XCUIElementTypeStepper[`name == 'search_adults_stepper'`]/XCUIElementTypeButton[`name == 'Decrement'`]")
            .build();
    private static final Locator SUBMIT = Locator.named("search.submit")
            .accessibilityId("search_submit_button")
            .predicate("type == 'XCUIElementTypeButton' AND label ==[c] 'Search'")
            .build();

    private static final int MAX_CALENDAR_SCROLLS = 13;   // ~ one year ahead

    @Override
    protected Locator screenAnchor() {
        return DESTINATION;
    }

    @Step("Search for {criteria.destination}")
    public SearchResultsPage search(SearchCriteria criteria) {
        enterDestination(criteria.destination());
        if (FrameworkConfig.get().getBoolean(ConfigKeys.SEARCH_SET_DATES, false)) {
            selectDates(criteria.checkIn(), criteria.checkOut());
        }
        if (FrameworkConfig.get().getBoolean(ConfigKeys.SEARCH_SET_GUESTS, false)) {
            setAdults(criteria.adults());
        }
        keyboard.dismiss();
        tap(SUBMIT);
        return new SearchResultsPage().waitForResults();
    }

    public SearchPage enterDestination(String destination) {
        type(DESTINATION, destination);
        // Autocomplete: choosing the suggestion is what a real user does and avoids ambiguous free text.
        if (isVisible(FIRST_SUGGESTION, Waits.shortWait())) {
            tap(FIRST_SUGGESTION);
        }
        return this;
    }

    /**
     * Selects dates on a calendar whose day cells are labelled per iOS accessibility conventions,
     * e.g. "Friday, October 16". The label pattern is configurable because it varies by app and locale.
     */
    public SearchPage selectDates(LocalDate checkIn, LocalDate checkOut) {
        tap(DATES);
        WebElement calendar = find.visible(CALENDAR, Waits.explicit());
        tapDay(calendar, checkIn);
        tapDay(calendar, checkOut);
        tap(CONFIRM_DATES);
        return this;
    }

    public SearchPage setAdults(int target) {
        // Bounded "act until state" loop: one stepper tap per poll until the displayed value matches.
        Waits.untilTrue(() -> {
            int current = currentAdults();
            if (current == target) {
                return true;
            }
            tap(current < target ? ADULTS_INCREMENT : ADULTS_DECREMENT);
            return false;
        }, Waits.explicit(), "adults stepper to reach " + target);
        return this;
    }

    private int currentAdults() {
        String digits = textOf(ADULTS_VALUE).replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private void tapDay(WebElement calendar, LocalDate date) {
        String pattern = FrameworkConfig.get().getString(ConfigKeys.SEARCH_CALENDAR_DAY_PATTERN, "EEEE, MMMM d");
        String label = date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
        Locator day = Locator.byLabel("search.calendarDay", label);
        ElementFinder inCalendar = new ElementFinder(calendar, "Calendar");

        for (int scrolls = 0; scrolls <= MAX_CALENDAR_SCROLLS; scrolls++) {
            var cell = inCalendar.findNow(day).filter(WebElement::isDisplayed);
            if (cell.isPresent()) {
                cell.get().click();
                return;
            }
            gestures.scroll(Gestures.Direction.DOWN, calendar);
        }
        throw new IllegalStateException("Day '" + label + "' not found in calendar after " + MAX_CALENDAR_SCROLLS + " scrolls");
    }
}
