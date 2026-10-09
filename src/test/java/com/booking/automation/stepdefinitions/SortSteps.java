package com.booking.automation.stepdefinitions;

import com.booking.automation.context.ScenarioContext;
import com.booking.automation.data.TestDataRepository;
import com.booking.automation.models.ResultItem;
import com.booking.automation.models.SortOption;
import com.booking.automation.utils.Evidence;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class SortSteps {

    private final ScenarioContext context;
    private final TestDataRepository data = TestDataRepository.get();

    public SortSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("the user sorts the results using a selected sorting option")
    public void theUserSortsUsingSelectedOption() {
        sortBy(data.defaultSortOption());
    }

    @When("the user sorts the results by {sortOrder}")
    public void theUserSortsBy(SortOption option) {
        sortBy(option);
    }

    @Then("the results should be displayed according to the selected sorting criteria")
    public void resultsShouldBeSortedBySelectedCriteria() {
        SortOption option = context.appliedSort();
        List<ResultItem> results = context.resultsPage().harvest();
        Evidence.text("Observed order for " + option, results.stream()
                .map(item -> option.key().apply(item) + "  <-  " + item.name())
                .collect(Collectors.joining("\n")));

        assertThat(results).as("sorting by %s keeps results on screen", option).isNotEmpty();
        // Secondary signal: if the app displays the active sort, it must be the one chosen.
        context.resultsPage().sortSummary().ifPresent(summary -> assertThat(summary)
                .as("results screen shows the active sort option")
                .containsIgnoringCase(data.sortLabel(option)));

        if (option.isVerifiable()) {
            // Business rule: adjacent pairs are in order. Reports the exact pair that breaks it.
            assertThat(option.firstViolation(results))
                    .as("results are ordered by %s (first out-of-order pair shown if any)", option)
                    .isEmpty();
        }
    }

    private void sortBy(SortOption option) {
        context.resultsPage(context.resultsPage().openSort().sortBy(data.sortLabel(option)));
        context.appliedSort(option);
    }
}
