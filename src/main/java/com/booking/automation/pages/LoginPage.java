package com.booking.automation.pages;

import com.booking.automation.components.AlertComponent;
import com.booking.automation.locators.Locator;
import com.booking.automation.models.Credentials;
import com.booking.automation.utils.Waits;
import io.qameta.allure.Step;

import java.util.Optional;

public class LoginPage extends BasePage {

    private static final Locator USERNAME = Locator.named("login.username")
            .accessibilityId("login_username_field")
            .predicate("type == 'XCUIElementTypeTextField' AND (placeholderValue CONTAINS[c] 'email' OR placeholderValue CONTAINS[c] 'user')")
            .build();
    private static final Locator PASSWORD = Locator.named("login.password")
            .accessibilityId("login_password_field")
            .classChain("**/XCUIElementTypeSecureTextField")
            .build();
    private static final Locator SUBMIT = Locator.named("login.submit")
            .accessibilityId("login_submit_button")
            .predicate("type == 'XCUIElementTypeButton' AND (label ==[c] 'Log in' OR label ==[c] 'Login' OR label ==[c] 'Sign in')")
            .build();
    private static final Locator ERROR_MESSAGE = Locator.named("login.error")
            .accessibilityId("login_error_message")
            .predicate("type == 'XCUIElementTypeStaticText' AND (label CONTAINS[c] 'invalid' OR label CONTAINS[c] 'incorrect' OR label CONTAINS[c] 'required')")
            .build();

    private final AlertComponent alerts = new AlertComponent(driver);

    @Override
    protected Locator screenAnchor() {
        return USERNAME;
    }

    public LoginPage open() {
        return waitUntilLoaded();
    }

    @Step("Log in as {credentials.username}")
    public HomePage loginAs(Credentials credentials) {
        submitCredentials(credentials);
        // iOS may offer to save the password to the keychain; it must not block the journey.
        alerts.dismissIfShown("Not Now", Waits.shortWait());
        return new HomePage().waitUntilLoaded();
    }

    /** Submits credentials without assuming success (negative scenarios). */
    @Step("Submit credentials for {credentials.username}")
    public LoginPage submitCredentials(Credentials credentials) {
        enterUsername(credentials.username());
        enterPassword(credentials.password());
        submit();
        return this;
    }

    public LoginPage enterUsername(String username) {
        type(USERNAME, username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        type(PASSWORD, password);   // SecureTextField: value is masked by iOS, never logged by us
        return this;
    }

    public void submit() {
        keyboard.dismiss();         // the keyboard can cover the button on small devices (iPhone SE)
        tap(SUBMIT);
    }

    public boolean isKeyboardShown() {
        return keyboard.isShown();
    }

    public LoginPage focusUsername() {
        find.visible(USERNAME, Waits.explicit()).click();
        keyboard.waitUntilShown();
        return this;
    }

    public boolean isSubmitEnabled() {
        return isEnabled(SUBMIT);
    }

    /** Error text shown inline or as an alert, whichever the app uses. */
    public Optional<String> errorMessage() {
        loading.waitUntilGone(Waits.longWait());
        if (isVisible(ERROR_MESSAGE, Waits.explicit())) {
            return Optional.of(textOf(ERROR_MESSAGE));
        }
        return alerts.textIfShown(Waits.shortWait());
    }
}
