package com.booking.automation.stepdefinitions;

import com.booking.automation.context.BookingJourney;
import com.booking.automation.data.TestDataRepository;
import com.booking.automation.pages.HomePage;
import com.booking.automation.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.assertj.core.api.Assertions.assertThat;

public class LoginSteps {

    private final BookingJourney journey;
    private final TestDataRepository data = TestDataRepository.get();
    private LoginPage loginPage;
    private HomePage homePage;

    public LoginSteps(BookingJourney journey) {
        this.journey = journey;
    }

    @Given("the user launches the Booking application")
    public void theUserLaunchesTheApplication() {
        loginPage = new LoginPage().open();
    }

    @Given("the user is logged in")
    public void theUserIsLoggedIn() {
        homePage = journey.loggedIn();
    }

    @When("the user logs in with valid credentials")
    public void theUserLogsInWithValidCredentials() {
        homePage = loginPage().loginAs(data.credentials("valid"));
    }

    @When("the user attempts to log in with {string} credentials")
    public void theUserAttemptsToLogIn(String alias) {
        loginPage().submitCredentials(data.credentials(alias));
    }

    @When("the user starts entering their username")
    public void theUserStartsEnteringTheirUsername() {
        loginPage().focusUsername();
    }

    @Then("the keyboard should be displayed")
    public void theKeyboardShouldBeDisplayed() {
        assertThat(loginPage().isKeyboardShown())
                .as("software keyboard is shown when the username field is focused")
                .isTrue();
    }

    @Then("the user should be successfully logged in")
    public void theUserShouldBeSuccessfullyLoggedIn() {
        assertThat(homePage).as("login step returned the landing screen").isNotNull();
        assertThat(homePage.isUserLoggedIn())
                .as("landing screen with search entry point is displayed after login")
                .isTrue();
    }

    @Then("the login should be rejected with the {string} error")
    public void theLoginShouldBeRejected(String messageKey) {
        assertThat(loginPage().errorMessage())
                .as("an error explains why login was rejected")
                .hasValueSatisfying(message -> assertThat(message).containsIgnoringCase(data.expectedMessage(messageKey)));
    }

    @Then("the user should remain on the login screen")
    public void theUserShouldRemainOnTheLoginScreen() {
        assertThat(loginPage().isDisplayed()).as("login screen is still displayed").isTrue();
    }

    private LoginPage loginPage() {
        if (loginPage == null) {
            loginPage = new LoginPage().open();
        }
        return loginPage;
    }
}
