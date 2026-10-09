package com.booking.automation.pages;

import com.booking.automation.components.ResultCardComponent;
import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.models.ResultItem;
import com.booking.automation.utils.Gestures;
import com.booking.automation.utils.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Search results list.
 *
 * <p>iOS table/collection views only keep visible cells in the accessibility tree, so verifying a
 * sort or filter on "the first 3 visible cards" proves little. {@link #harvest(int)} scrolls and
 * collects a configurable sample (default 10), de-duplicating by name, which gives a meaningful
 * oracle without scrolling through hundreds of results.</p>
 */
public class SearchResultsPage extends BasePage {

    private static final Locator RESULTS_LIST = Locator.named("results.list")
            .accessibilityId("search_results_list")
            .classChain("**/XCUIElementTypeTable")
            .classChain("**/XCUIElementTypeCollectionView")
            .build();
    private static final Locator RESULT_CELLS = Locator.named("results.cells")
            .classChain("**/XCUIElementTypeCell[`name BEGINSWITH 'result_card'`]")
            .classChain("**/XCUIElementTypeTable/XCUIElementTypeCell")
            .classChain("**/XCUIElementTypeCollectionView/XCUIElementTypeCell")
            .build();
    private static final Locator EMPTY_STATE = Locator.named("results.emptyState")
            .accessibilityId("search_results_empty")
            .predicate("label CONTAINS[c] 'no results' OR label CONTAINS[c] 'no properties'")
            .build();
    private static final Locator RESULTS_COUNT = Locator.named("results.count")
            .accessibilityId("search_results_count")
            .predicate("type == 'XCUIElementTypeStaticText' AND label MATCHES '.*[0-9]+ (results|properties|stays).*'")
            .build();
    private static final Locator FILTER_BUTTON = Locator.named("results.filterButton")
            .accessibilityId("search_results_filter_button")
            .predicate("type == 'XCUIElementTypeButton' AND label BEGINSWITH[c] 'Filter'")
            .build();
    private static final Locator SORT_BUTTON = Locator.named("results.sortButton")
            .accessibilityId("search_results_sort_button")
            .predicate("type == 'XCUIElementTypeButton' AND label BEGINSWITH[c] 'Sort'")
            .build();
    private static final Locator ACTIVE_FILTER_CHIPS = Locator.named("results.activeFilterChips")
            .accessibilityId("active_filter_chip")
            .build();
    private static final Locator SORT_SUMMARY = Locator.named("results.sortSummary")
            .accessibilityId("search_results_sort_summary")
            .build();

    private final int sampleSize;
    private final int maxScrolls;

    public SearchResultsPage() {
        FrameworkConfig config = FrameworkConfig.get();
        this.sampleSize = config.getInt(ConfigKeys.RESULTS_SAMPLE_SIZE, 10);
        this.maxScrolls = config.getInt(ConfigKeys.RESULTS_MAX_SCROLLS, 8);
    }

    @Override
    protected Locator screenAnchor() {
        return RESULTS_LIST;
    }

    /** Waits for loading to finish and for either results or an explicit empty state - never a fixed delay. */
    public SearchResultsPage waitForResults() {
        loading.waitUntilGone(Waits.longWait());
        Waits.untilTrue(() -> !find.findAllNow(RESULT_CELLS).isEmpty() || find.findNow(EMPTY_STATE).isPresent(),
                Waits.longWait(), "search results or an empty state");
        // Lists often render progressively; wait until the visible cell count settles.
        Waits.untilStable(() -> find.findAllNow(RESULT_CELLS).size(), Waits.explicit(), "result cell count");
        return this;
    }

    public boolean hasResults() {
        return !find.findAllNow(RESULT_CELLS).isEmpty();
    }

    public boolean isEmptyStateShown() {
        return find.findNow(EMPTY_STATE).isPresent();
    }

    public Optional<String> resultsCountText() {
        return find.findNow(RESULTS_COUNT).map(ElementFinder::readText);
    }

    /** Results currently on screen, top to bottom. */
    public List<ResultItem> visibleResults() {
        List<WebElement> cells = find.findAllNow(RESULT_CELLS);
        List<ResultItem> items = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            items.add(new ResultCardComponent(cells.get(i)).read(i));
        }
        return items;
    }

    /** Collects the configured sample size. */
    public List<ResultItem> harvest() {
        return harvest(sampleSize);
    }

    /**
     * Scrolls the list collecting up to {@code limit} distinct results, stopping early when a scroll
     * reveals nothing new (end of list). Leaves the list scrolled back to the top.
     */
    @Step("Collect up to {limit} results from the list")
    public List<ResultItem> harvest(int limit) {
        Map<String, ResultItem> collected = new LinkedHashMap<>();
        WebElement list = find.visible(RESULTS_LIST, Waits.explicit());

        for (int scroll = 0; scroll <= maxScrolls && collected.size() < limit; scroll++) {
            int before = collected.size();
            for (ResultItem item : visibleResults()) {
                if (!item.name().isBlank() && collected.size() < limit) {
                    collected.putIfAbsent(item.name(), withPosition(item, collected.size()));
                }
            }
            if (scroll > 0 && collected.size() == before) {
                break;   // end of list reached
            }
            gestures.scroll(Gestures.Direction.DOWN, list);
            Waits.untilStable(this::firstVisibleName, Waits.shortWait(), "list after scroll");
        }
        scrollToTop(list);
        log.info("Harvested {} result(s)", collected.size());
        return new ArrayList<>(collected.values());
    }

    @Step("Open result '{item.name}'")
    public BookingDetailsPage open(ResultItem item) {
        WebElement list = find.visible(RESULTS_LIST, Waits.explicit());
        for (int scroll = 0; scroll <= maxScrolls; scroll++) {
            for (WebElement cell : find.findAllNow(RESULT_CELLS)) {
                ResultCardComponent card = new ResultCardComponent(cell);
                if (card.name().equals(item.name()) && card.isFullyVisible()) {
                    card.open();
                    return new BookingDetailsPage().waitUntilLoaded();
                }
            }
            gestures.scroll(Gestures.Direction.DOWN, list);
        }
        throw new IllegalStateException("Result '" + item.name() + "' could not be found to open");
    }

    @Step("Open filters")
    public FilterPage openFilters() {
        tap(FILTER_BUTTON);
        return new FilterPage().waitUntilLoaded();
    }

    @Step("Open sort options")
    public SortPage openSort() {
        tap(SORT_BUTTON);
        return new SortPage().waitUntilLoaded();
    }

    /** Labels of filters shown as applied (chips / badges) on the results screen. */
    public List<String> appliedFilterLabels() {
        return find.findAllNow(ACTIVE_FILTER_CHIPS).stream().map(ElementFinder::readText).toList();
    }

    /** Filter button label often carries a count, e.g. "Filters (1)". */
    public String filterButtonLabel() {
        return textOf(FILTER_BUTTON);
    }

    /** Text describing the active sort, if the app shows one (e.g. "Sorted by: Price (lowest first)"). */
    public Optional<String> sortSummary() {
        return find.findNow(SORT_SUMMARY).map(ElementFinder::readText);
    }

    private String firstVisibleName() {
        List<WebElement> cells = find.findAllNow(RESULT_CELLS);
        return cells.isEmpty() ? "" : new ResultCardComponent(cells.get(0)).name();
    }

    private void scrollToTop(WebElement list) {
        for (int i = 0; i <= maxScrolls; i++) {
            String before = firstVisibleName();
            gestures.scroll(Gestures.Direction.UP, list);
            if (Waits.untilStable(this::firstVisibleName, Waits.shortWait(), "list after scroll up").equals(before)) {
                return;
            }
        }
    }

    private static ResultItem withPosition(ResultItem item, int position) {
        return new ResultItem(position, item.name(), item.location(), item.price(), item.rating(),
                item.propertyType(), item.available());
    }
}
