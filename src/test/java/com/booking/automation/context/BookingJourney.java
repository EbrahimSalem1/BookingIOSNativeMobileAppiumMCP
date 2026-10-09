package com.booking.automation.context;

import com.booking.automation.data.TestDataRepository;
import com.booking.automation.models.SearchCriteria;
import com.booking.automation.pages.HomePage;
import com.booking.automation.pages.LoginPage;
import com.booking.automation.pages.SearchResultsPage;

/**
 * Reusable preconditions ("Given the user is logged in", "Given search results are displayed").
 *
 * <p>Steps never call other steps; shared journeys live here instead, so preconditions are written
 * once and stay fast to change. A natural next optimisation is replacing the UI login with a
 * deep link / launch argument that injects a session token (see docs/ARCHITECTURE.md).</p>
 */
public class BookingJourney {

    private final ScenarioContext context;
    private final TestDataRepository data = TestDataRepository.get();

    public BookingJourney(ScenarioContext context) {
        this.context = context;
    }

    public HomePage loggedIn() {
        return new LoginPage().open().loginAs(data.credentials("valid"));
    }

    public SearchResultsPage searchResultsFor(SearchCriteria criteria) {
        SearchResultsPage results = new HomePage().openSearch().search(criteria);
        context.searchCriteria(criteria);
        context.resultsPage(results);
        return results;
    }

    public SearchResultsPage loggedInWithDefaultResults() {
        loggedIn();
        return searchResultsFor(data.defaultSearch());
    }
}
