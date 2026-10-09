package com.booking.automation.stepdefinitions;

import com.booking.automation.context.ScenarioContext;
import com.booking.automation.data.TestDataRepository;
import com.booking.automation.models.BookingDetails;
import com.booking.automation.models.ResultItem;
import com.booking.automation.pages.BookingDetailsPage;
import com.booking.automation.utils.Evidence;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.SoftAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingDetailsSteps {

    private final ScenarioContext context;
    private final TestDataRepository data = TestDataRepository.get();
    private BookingDetailsPage detailsPage;

    public BookingDetailsSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("the user selects a booking option")
    public void theUserSelectsABookingOption() {
        ResultItem chosen = data.selectionStrategy().choose(context.resultsPage().harvest())
                .orElseThrow(() -> new AssertionError("No bookable result (name + price + available) in the list"));
        context.selectedResult(chosen);
        Evidence.text("Selected result", chosen.toString());
        detailsPage = context.resultsPage().open(chosen);
    }

    @Then("the booking details screen should be displayed")
    public void theBookingDetailsScreenShouldBeDisplayed() {
        assertThat(detailsPage.isDisplayed()).as("booking details screen is displayed").isTrue();
    }

    /**
     * Verifies the details belong to the result the user tapped - the "logical connection" between
     * list and details - rather than comparing against static expected values. All mismatches are
     * reported together (soft assertions) so one run shows the full picture.
     */
    @Then("the selected booking information should be displayed correctly")
    public void theSelectedBookingInformationShouldBeDisplayedCorrectly() {
        ResultItem selected = context.selectedResult();
        BookingDetails details = detailsPage.read();
        Evidence.text("Selected vs details", "Selected: " + selected + "\nDetails:  " + details);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(details.name()).as("property name").isEqualTo(selected.name());
        if (!selected.location().isBlank() && !details.location().isBlank()) {
            softly.assertThat(details.location()).as("location").containsIgnoringCase(selected.location());
        }
        if (selected.price().isPresent()) {
            softly.assertThat(details.price()).as("price shown on details").isPresent();
            details.price().ifPresent(price -> softly.assertThat(price)
                    .as("price on details matches the card (same stay, same dates)")
                    .isEqualByComparingTo(selected.price().get()));
        }
        selected.rating().ifPresent(rating -> softly.assertThat(details.rating())
                .as("rating").hasValue(rating));
        softly.assertThat(details.imageCount()).as("at least one property image").isPositive();
        softly.assertThat(details.bookActionAvailable()).as("a booking call-to-action is available").isTrue();
        softly.assertAll();
    }
}
