package com.booking.automation.models;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Sorting options and - crucially - the <b>business rule</b> each one promises.
 *
 * <p>The comparator is the oracle: after sorting, the harvested result list must be ordered by it.
 * Items missing the sort key (e.g. unrated properties) are expected at the end, which is the usual
 * product convention; if the product disagrees, that is a question for the PO, recorded in the
 * exploratory testing notes.</p>
 */
public enum SortOption {

    PRICE_LOW_TO_HIGH(Comparator.comparing(ResultItem::price, missingLast(Comparator.<BigDecimal>naturalOrder()))),
    PRICE_HIGH_TO_LOW(Comparator.comparing(ResultItem::price, missingLast(Comparator.<BigDecimal>reverseOrder()))),
    RATING_HIGH_TO_LOW(Comparator.comparing(ResultItem::rating, missingLast(Comparator.<Double>reverseOrder()))),
    /** Opaque ranking: order cannot be asserted, only that the option is applied and results remain. */
    RECOMMENDED(null);

    private final Comparator<ResultItem> oracle;

    SortOption(Comparator<ResultItem> oracle) {
        this.oracle = oracle;
    }

    public boolean isVerifiable() {
        return oracle != null;
    }

    public Comparator<ResultItem> oracle() {
        if (oracle == null) {
            throw new UnsupportedOperationException(name() + " has no deterministic ordering to verify");
        }
        return oracle;
    }

    /** Returns the first adjacent pair that violates the ordering, if any (for precise failure messages). */
    public Optional<List<ResultItem>> firstViolation(List<ResultItem> items) {
        for (int i = 1; i < items.size(); i++) {
            if (oracle().compare(items.get(i - 1), items.get(i)) > 0) {
                return Optional.of(List.of(items.get(i - 1), items.get(i)));
            }
        }
        return Optional.empty();
    }

    /** The key used for a human-readable "observed order" in reports. */
    public Function<ResultItem, Object> key() {
        return switch (this) {
            case PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW -> item -> item.price().map(BigDecimal::toPlainString).orElse("-");
            case RATING_HIGH_TO_LOW -> item -> item.rating().map(String::valueOf).orElse("-");
            case RECOMMENDED -> ResultItem::name;
        };
    }

    private static <T> Comparator<Optional<T>> missingLast(Comparator<T> comparator) {
        return (a, b) -> {
            if (a.isPresent() && b.isPresent()) {
                return comparator.compare(a.get(), b.get());
            }
            return a.isPresent() ? -1 : (b.isPresent() ? 1 : 0);
        };
    }
}
