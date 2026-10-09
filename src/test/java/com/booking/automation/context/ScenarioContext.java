package com.booking.automation.context;

import com.booking.automation.models.FilterCriteria;
import com.booking.automation.models.ResultItem;
import com.booking.automation.models.SearchCriteria;
import com.booking.automation.models.SortOption;
import com.booking.automation.pages.SearchResultsPage;

import java.util.List;
import java.util.Optional;

/**
 * Per-scenario state shared between step definition classes.
 *
 * <p>Created fresh for every scenario by Cucumber's PicoContainer and injected through constructors,
 * so state can never leak between scenarios or between parallel threads (no static fields).</p>
 */
public class ScenarioContext {

    private SearchCriteria searchCriteria;
    private SearchResultsPage resultsPage;
    private List<ResultItem> resultsBeforeFilter = List.of();
    private FilterCriteria appliedFilter;
    private SortOption appliedSort;
    private ResultItem selectedResult;

    public SearchCriteria searchCriteria() {
        return require(searchCriteria, "search criteria (did a search step run?)");
    }

    public void searchCriteria(SearchCriteria criteria) {
        this.searchCriteria = criteria;
    }

    public SearchResultsPage resultsPage() {
        return require(resultsPage, "results page (did a search step run?)");
    }

    public void resultsPage(SearchResultsPage page) {
        this.resultsPage = page;
    }

    public List<ResultItem> resultsBeforeFilter() {
        return resultsBeforeFilter;
    }

    public void resultsBeforeFilter(List<ResultItem> results) {
        this.resultsBeforeFilter = List.copyOf(results);
    }

    public FilterCriteria appliedFilter() {
        return require(appliedFilter, "applied filter (did a filter step run?)");
    }

    public Optional<FilterCriteria> appliedFilterIfAny() {
        return Optional.ofNullable(appliedFilter);
    }

    public void appliedFilter(FilterCriteria filter) {
        this.appliedFilter = filter;
    }

    public SortOption appliedSort() {
        return require(appliedSort, "applied sort option (did a sort step run?)");
    }

    public void appliedSort(SortOption sort) {
        this.appliedSort = sort;
    }

    public ResultItem selectedResult() {
        return require(selectedResult, "selected result (did a selection step run?)");
    }

    public void selectedResult(ResultItem item) {
        this.selectedResult = item;
    }

    private static <T> T require(T value, String what) {
        if (value == null) {
            throw new IllegalStateException("Scenario context has no " + what);
        }
        return value;
    }
}
