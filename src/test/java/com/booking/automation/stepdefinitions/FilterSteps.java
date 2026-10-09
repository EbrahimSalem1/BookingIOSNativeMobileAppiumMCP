package com.booking.automation.stepdefinitions;

import com.booking.automation.context.ScenarioContext;
import com.booking.automation.data.TestDataRepository;
import com.booking.automation.models.FilterCriteria;
import com.booking.automation.models.ResultItem;
import com.booking.automation.pages.SearchResultsPage;
import com.booking.automation.utils.Evidence;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class FilterSteps {

    private final ScenarioContext context;
    private final TestDataRepository data = TestDataRepository.get();

    public FilterSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("the user applies a valid filter")
    public void theUserAppliesAValidFilter() {
        apply(data.defaultFilter());
    }

    @When("the user applies the {string} filter")
    public void theUserAppliesTheFilter(String alias) {
        apply(data.filter(alias));
    }

    @When("the user views a stay and returns to the results")
    public void theUserViewsAStayAndReturns() {
        SearchResultsPage results = context.resultsPage();
        ResultItem any = data.selectionStrategy().choose(results.harvest())
                .orElseThrow(() -> new AssertionError("No bookable result to open"));
        context.resultsPage(results.open(any).back());
    }

    @Then("the filtered results should be displayed")
    public void theFilteredResultsShouldBeDisplayed() {
        FilterCriteria filter = context.appliedFilter();
        List<ResultItem> after = context.resultsPage().harvest();
        Evidence.text("Results after filter " + filter.describe(), join(after));

        assertThat(after).as("filter %s leaves at least one result", filter.describe()).isNotEmpty();

        // Business rule: every remaining result satisfies the filter.
        assertThat(after)
                .as("every result satisfies %s", filter.describe())
                .allMatch(filter.rule());

        // The list was actually updated: results that violated the rule before are gone.
        Set<String> violatorsBefore = context.resultsBeforeFilter().stream()
                .filter(filter.rule().negate()).map(ResultItem::name).collect(Collectors.toSet());
        if (violatorsBefore.isEmpty()) {
            Evidence.text("Test data note", "All results already satisfied " + filter.describe()
                    + " before filtering, so 'list updated' could not be proven. Consider a more selective filter.");
        } else {
            assertThat(after).as("results excluded by %s are no longer shown", filter.describe())
                    .noneMatch(item -> violatorsBefore.contains(item.name()));
        }
    }

    @Then("the selected filter should remain applied")
    public void theSelectedFilterShouldRemainApplied() {
        FilterCriteria filter = context.appliedFilter();
        SearchResultsPage results = context.resultsPage();
        List<String> chips = results.appliedFilterLabels();
        // Apps signal active filters either with chips or a counter on the button ("Filters (1)").
        boolean indicated = chips.stream().anyMatch(chip -> chip.equalsIgnoreCase(filter.uiLabel()))
                || results.filterButtonLabel().matches(".*\\b[1-9]\\d*\\b.*");   // non-zero active-filter count
        assertThat(indicated)
                .as("the results screen indicates that '%s' is applied (chips=%s, button='%s')",
                        filter.uiLabel(), chips, results.filterButtonLabel())
                .isTrue();
    }

    private void apply(FilterCriteria filter) {
        SearchResultsPage results = context.resultsPage();
        context.resultsBeforeFilter(results.harvest());
        context.resultsPage(results.openFilters().select(filter).apply());
        context.appliedFilter(filter);
    }

    private static String join(List<ResultItem> items) {
        return items.stream().map(ResultItem::toString).collect(Collectors.joining("\n"));
    }
}
