@search
Feature: Search for stays
  As a logged-in traveller
  I want to search for stays at my destination
  So that I only see options that match my trip

  Background:
    Given the user is logged in

  @smoke @critical
  Scenario: User searches for a booking option
    When the user searches using valid search criteria
    Then relevant search results should be displayed

  @regression
  Scenario Outline: Search results are relevant for <label>
    When the user searches for the "<trip>" trip
    Then relevant search results should be displayed

    Examples:
      | label                  | trip                 |
      | a London business trip | london_business_trip |
      | a Cairo family stay    | cairo_family         |

  @regression @negative
  Scenario: User is informed when nothing matches the search
    When the user searches for the "no_results" trip
    Then the user should be told that no stays match the search
