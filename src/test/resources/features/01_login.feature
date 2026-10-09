@login
Feature: Login
  As a registered traveller
  I want to sign in to the Booking app
  So that I can search for and book stays with my account

  @smoke @critical
  Scenario: User logs in successfully
    Given the user launches the Booking application
    When the user logs in with valid credentials
    Then the user should be successfully logged in

  @regression @negative
  Scenario Outline: Login is rejected for <case>
    Given the user launches the Booking application
    When the user attempts to log in with "<credentials>" credentials
    Then the login should be rejected with the "<message>" error
    And the user should remain on the login screen

    Examples:
      | case                  | credentials       | message                  |
      | a wrong password      | wrong_password    | login.invalidCredentials |
      | an unregistered user  | unregistered_user | login.invalidCredentials |

  @regression @ux
  Scenario: Keyboard appears for credential entry and does not block sign in
    Given the user launches the Booking application
    When the user starts entering their username
    Then the keyboard should be displayed
    When the user logs in with valid credentials
    Then the user should be successfully logged in
