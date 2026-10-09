@details
Feature: Booking details
  As a traveller who found an interesting stay
  I want to open its details
  So that I can confirm it is the stay I chose before booking

  Background:
    Given search results are displayed

  @smoke @critical
  Scenario: User opens booking details
    When the user selects a booking option
    Then the booking details screen should be displayed
    And the selected booking information should be displayed correctly
