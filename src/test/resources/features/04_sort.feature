@sort
Feature: Sort search results
  As a traveller comparing stays
  I want to order the results by what matters to me
  So that the best options for me appear first

  Background:
    Given search results are displayed

  @smoke @critical
  Scenario: User sorts search results
    When the user sorts the results using a selected sorting option
    Then the results should be displayed according to the selected sorting criteria

  @regression
  Scenario Outline: Results are ordered by <order>
    When the user sorts the results by <order>
    Then the results should be displayed according to the selected sorting criteria

    Examples:
      | order              |
      | price low to high  |
      | price high to low  |
      | rating             |

  @regression
  Scenario: Sorting keeps the active filter
    When the user applies a valid filter
    And the user sorts the results by price low to high
    Then the results should be displayed according to the selected sorting criteria
    And the filtered results should be displayed
