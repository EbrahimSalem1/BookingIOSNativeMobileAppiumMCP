package com.booking.automation.unit;

import com.booking.automation.models.FilterCriteria;
import com.booking.automation.models.ResultItem;
import com.booking.automation.models.SelectionStrategy;
import com.booking.automation.models.SortOption;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The oracles decide whether the app is right. If an oracle is wrong, every sort/filter test is
 * meaningless - so the oracles themselves are tested, without a device.
 */
public class SortAndFilterOracleTest {

    private static ResultItem item(String name, String price, Double rating) {
        return new ResultItem(0, name, "Paris, France", Optional.ofNullable(price).map(BigDecimal::new),
                Optional.ofNullable(rating), "Hotel", true);
    }

    @Test
    public void priceLowToHighAcceptsAscendingAndRejectsDescending() {
        List<ResultItem> ascending = List.of(item("A", "90", 4.0), item("B", "120.50", 3.0), item("C", "300", 5.0));
        assertThat(SortOption.PRICE_LOW_TO_HIGH.firstViolation(ascending)).isEmpty();

        List<ResultItem> broken = List.of(item("A", "90", 4.0), item("B", "300", 3.0), item("C", "120", 5.0));
        assertThat(SortOption.PRICE_LOW_TO_HIGH.firstViolation(broken))
                .hasValueSatisfying(pair -> assertThat(pair).extracting(ResultItem::name).containsExactly("B", "C"));
    }

    @Test
    public void numericNotLexicalOrdering() {
        // "1200" < "300" lexically; the oracle must compare numbers.
        assertThat(SortOption.PRICE_LOW_TO_HIGH.firstViolation(List.of(item("A", "300", null), item("B", "1200", null))))
                .isEmpty();
    }

    @Test
    public void itemsWithoutSortKeyAreExpectedLast() {
        List<ResultItem> unratedLast = List.of(item("A", "1", 4.8), item("B", "1", 4.1), item("C", "1", null));
        assertThat(SortOption.RATING_HIGH_TO_LOW.firstViolation(unratedLast)).isEmpty();

        List<ResultItem> unratedFirst = List.of(item("C", "1", null), item("A", "1", 4.8));
        assertThat(SortOption.RATING_HIGH_TO_LOW.firstViolation(unratedFirst)).isPresent();
    }

    @Test
    public void recommendedIsNotVerifiable() {
        assertThat(SortOption.RECOMMENDED.isVerifiable()).isFalse();
        assertThatThrownBy(SortOption.RECOMMENDED::oracle).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void priceMaxFilterIsInclusiveAndRejectsUnpricedResults() {
        FilterCriteria under200 = new FilterCriteria(FilterCriteria.Type.PRICE_MAX, "Under $200", "200");
        assertThat(under200.rule()).accepts(item("A", "200", null), item("B", "199.99", null));
        assertThat(under200.rule()).rejects(item("C", "200.01", null), item("D", null, null));
    }

    @Test
    public void minRatingFilter() {
        FilterCriteria fourPlus = new FilterCriteria(FilterCriteria.Type.MIN_RATING, "4+", "4");
        assertThat(fourPlus.rule()).accepts(item("A", "1", 4.0)).rejects(item("B", "1", 3.9), item("C", "1", null));
    }

    @Test
    public void selectionSkipsUnbookableCards() {
        ResultItem soldOut = new ResultItem(0, "Sold", "Paris", Optional.of(new BigDecimal("50")), Optional.of(5.0), "Hotel", false);
        ResultItem noPrice = item("NoPrice", null, 4.0);
        ResultItem good = item("Good", "150", 4.2);
        ResultItem cheap = item("Cheap", "80", 3.5);

        List<ResultItem> list = List.of(soldOut, noPrice, good, cheap);
        assertThat(SelectionStrategy.FIRST_BOOKABLE.choose(list)).contains(good);
        assertThat(SelectionStrategy.CHEAPEST.choose(list)).contains(cheap);
        assertThat(SelectionStrategy.HIGHEST_RATED.choose(list)).contains(good);
    }
}
