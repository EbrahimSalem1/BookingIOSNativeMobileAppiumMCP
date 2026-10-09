package com.booking.automation.components;

import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.models.ResultItem;
import com.booking.automation.utils.TextParsers;
import org.openqa.selenium.WebElement;

/**
 * One result card (an {@code XCUIElementTypeCell}) in the search results list.
 *
 * <p>Child locators are resolved <em>relative to the cell</em>, never globally, so that
 * "price" always means "the price of this card". This is what makes the selected result and
 * the details screen provably connected.</p>
 */
public class ResultCardComponent {

    static final Locator NAME = Locator.named("resultCard.name")
            .accessibilityId("result_card_name")
            .classChain("**/XCUIElementTypeStaticText[1]")
            .build();
    static final Locator LOCATION = Locator.named("resultCard.location")
            .accessibilityId("result_card_location")
            .classChain("**/XCUIElementTypeStaticText[2]")
            .build();
    static final Locator PRICE = Locator.named("resultCard.price")
            .accessibilityId("result_card_price")
            .predicate("type == 'XCUIElementTypeStaticText' AND (label MATCHES '.*[$€£].*[0-9].*' OR label CONTAINS[c] 'EGP' OR label CONTAINS[c] 'per night')")
            .build();
    static final Locator RATING = Locator.named("resultCard.rating")
            .accessibilityId("result_card_rating")
            .predicate("type == 'XCUIElementTypeStaticText' AND label MATCHES '^[0-9]+([.,][0-9])?(/10)?$'")
            .build();
    static final Locator PROPERTY_TYPE = Locator.named("resultCard.propertyType")
            .accessibilityId("result_card_type")
            .build();
    static final Locator UNAVAILABLE_BADGE = Locator.named("resultCard.unavailableBadge")
            .accessibilityId("result_card_sold_out")
            .predicate("label CONTAINS[c] 'sold out' OR label CONTAINS[c] 'unavailable'")
            .build();

    private final WebElement cell;
    private final ElementFinder within;

    public ResultCardComponent(WebElement cell) {
        this.cell = cell;
        this.within = new ElementFinder(cell, "ResultCard");
    }

    public String name() {
        return within.textOrEmpty(NAME);
    }

    /** Snapshot of everything the user can see on this card. */
    public ResultItem read(int position) {
        return new ResultItem(
                position,
                name(),
                within.textOrEmpty(LOCATION),
                TextParsers.price(within.textOrEmpty(PRICE)),
                TextParsers.rating(within.textOrEmpty(RATING)),
                within.textOrEmpty(PROPERTY_TYPE),
                within.findNow(UNAVAILABLE_BADGE).isEmpty());
    }

    public boolean isFullyVisible() {
        return cell.isDisplayed();
    }

    public void open() {
        cell.click();
    }
}
