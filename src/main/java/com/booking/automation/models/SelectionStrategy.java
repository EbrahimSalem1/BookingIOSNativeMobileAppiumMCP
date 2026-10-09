package com.booking.automation.models;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * How a scenario picks "a valid result". Picking the first card blindly makes tests fail on
 * sponsored, sold-out or price-less cards; the strategy makes the choice explicit and data driven.
 */
public enum SelectionStrategy {

    FIRST_BOOKABLE {
        @Override
        public Optional<ResultItem> choose(List<ResultItem> items) {
            return items.stream().filter(ResultItem::isBookable).findFirst();
        }
    },
    CHEAPEST {
        @Override
        public Optional<ResultItem> choose(List<ResultItem> items) {
            return items.stream().filter(ResultItem::isBookable)
                    .min(Comparator.comparing(item -> item.price().orElseThrow()));
        }
    },
    HIGHEST_RATED {
        @Override
        public Optional<ResultItem> choose(List<ResultItem> items) {
            return items.stream().filter(ResultItem::isBookable).filter(item -> item.rating().isPresent())
                    .max(Comparator.comparing(item -> item.rating().orElseThrow()));
        }
    };

    public abstract Optional<ResultItem> choose(List<ResultItem> items);
}
