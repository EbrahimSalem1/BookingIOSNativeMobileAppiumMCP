@e2e
Feature: End-to-end booking discovery journey
  The main user journey, from launch to a verified details screen, as one release-gate scenario.

  @smoke @critical
  Scenario: Traveller finds, refines and opens a stay
    Given the user launches the Booking application
    When the user logs in with valid credentials
    And the user searches using valid search criteria
    Then relevant search results should be displayed
    When the user applies a valid filter
    Then the filtered results should be displayed
    When the user sorts the results using a selected sorting option
    Then the results should be displayed according to the selected sorting criteria
    And the filtered results should be displayed
    When the user selects a booking option
    Then the booking details screen should be displayed
    And the selected booking information should be displayed correctly
