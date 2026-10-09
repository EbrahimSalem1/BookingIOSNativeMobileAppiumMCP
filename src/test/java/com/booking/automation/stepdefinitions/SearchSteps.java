package com.booking.automation.stepdefinitions;

import com.booking.automation.context.BookingJourney;
import com.booking.automation.context.ScenarioContext;
import com.booking.automation.data.TestDataRepository;
import com.booking.automation.models.ResultItem;
import com.booking.automation.models.SearchCriteria;
import com.booking.automation.utils.Evidence;
import com.booking.automation.utils.TextParsers;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchSteps {

    private final ScenarioContext context;
    private final BookingJourney journey;
    private final TestDataRepository data = TestDataRepository.get();

    public SearchSteps(ScenarioContext context, BookingJourney journey) {
        this.context = context;
        this.journey = journey;
    }

    @Given("search results are displayed")
    public void searchResultsAreDisplayed() {
        journey.loggedInWithDefaultResults();
        assertThat(context.resultsPage().hasResults())
                .as("precondition: default search '%s' returns results", context.searchCriteria().destination())
                .isTrue();
    }

    @When("the user searches using valid search criteria")
    public void theUserSearchesUsingValidCriteria() {
        journey.searchResultsFor(data.defaultSearch());
    }

    @When("the user searches for the {string} trip")
    public void theUserSearchesForTrip(String alias) {
        journey.searchResultsFor(data.search(alias));
    }

    @Then("relevant search results should be displayed")
    public void relevantSearchResultsShouldBeDisplayed() {
        SearchCriteria criteria = context.searchCriteria();
        assertThat(context.resultsPage().isDisplayed()).as("results screen is displayed").isTrue();

        List<ResultItem> results = context.resultsPage().harvest();
        Evidence.text("Harvested results", results.stream().map(ResultItem::toString).collect(Collectors.joining("\n")));

        assertThat(results).as("search for '%s' returns results", criteria.destination()).isNotEmpty();
        // Relevance oracle: every sampled result must relate to what the user searched for.
        assertThat(results)
                .as("every result is located in / related to '%s'", criteria.relevanceKeyword())
                .allSatisfy(item -> assertThat(
                        TextParsers.containsIgnoringCase(item.location(), criteria.relevanceKeyword())
                                || TextParsers.containsIgnoringCase(item.name(), criteria.relevanceKeyword()))
                        .as("result %s mentions '%s'", item, criteria.relevanceKeyword())
                        .isTrue());
    }

    @Then("the user should be told that no stays match the search")
    public void theUserShouldBeToldNoStaysMatch() {
        assertThat(context.resultsPage().isEmptyStateShown())
                .as("an explicit empty state is shown instead of a blank list")
                .isTrue();
        assertThat(context.resultsPage().hasResults()).as("no result cards are shown").isFalse();
    }
}
