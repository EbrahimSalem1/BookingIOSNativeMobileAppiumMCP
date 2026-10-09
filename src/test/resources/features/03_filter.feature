@filter
Feature: Filter search results
  As a traveller with a budget and preferences
  I want to narrow the search results
  So that I only see stays I would actually book

  Background:
    Given search results are displayed

  @smoke @critical
  Scenario: User filters search results
    When the user applies a valid filter
    Then the filtered results should be displayed
    And the selected filter should remain applied

  @regression
  Scenario Outline: Every result respects the "<filter>" filter
    When the user applies the "<filter>" filter
    Then the filtered results should be displayed
    And the selected filter should remain applied

    Examples:
      | filter         |
      | rating_4_plus  |
      | hotels_only    |

  @regression
  Scenario: Applied filter survives viewing a stay and coming back
    When the user applies a valid filter
    And the user views a stay and returns to the results
    Then the selected filter should remain applied
    And the filtered results should be displayed
