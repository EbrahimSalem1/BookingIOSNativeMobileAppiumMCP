package com.booking.automation.pages;

import com.booking.automation.locators.ElementFinder;
import com.booking.automation.locators.Locator;
import com.booking.automation.models.BookingDetails;
import com.booking.automation.utils.TextParsers;
import io.qameta.allure.Step;

public class BookingDetailsPage extends BasePage {

    private static final Locator CONTAINER = Locator.named("details.container")
            .accessibilityId("booking_details_screen")
            // Fallback: the primary booking CTA only exists on the details screen.
            .predicate("type == 'XCUIElementTypeButton' AND (label BEGINSWITH[c] 'Book' OR label BEGINSWITH[c] 'Reserve')")
            .build();
    private static final Locator NAME = Locator.named("details.name")
            .accessibilityId("details_property_name")
            .classChain("**/XCUIElementTypeScrollView/**/XCUIElementTypeStaticText[1]")
            .build();
    private static final Locator LOCATION = Locator.named("details.location")
            .accessibilityId("details_location")
            .build();
    private static final Locator PRICE = Locator.named("details.price")
            .accessibilityId("details_price")
            .build();
    private static final Locator RATING = Locator.named("details.rating")
            .accessibilityId("details_rating")
            .build();
    private static final Locator GALLERY_IMAGES = Locator.named("details.galleryImages")
            .classChain("**/XCUIElementTypeCollectionView[`name == 'details_gallery'`]/XCUIElementTypeCell")
            .classChain("**/XCUIElementTypeImage")
            .build();
    private static final Locator BOOK_BUTTON = Locator.named("details.book")
            .accessibilityId("details_book_button")
            .predicate("type == 'XCUIElementTypeButton' AND (label BEGINSWITH[c] 'Book' OR label BEGINSWITH[c] 'Reserve')")
            .build();
    private static final Locator BACK = Locator.named("details.back")
            .classChain("**/XCUIElementTypeNavigationBar/XCUIElementTypeButton[1]")
            .build();

    @Override
    protected Locator screenAnchor() {
        return CONTAINER;
    }

    @Step("Read booking details")
    public BookingDetails read() {
        return new BookingDetails(
                textOf(NAME),
                find.findNow(LOCATION).map(ElementFinder::readText).orElse(""),
                TextParsers.price(find.findNow(PRICE).map(ElementFinder::readText).orElse("")),
                TextParsers.rating(find.findNow(RATING).map(ElementFinder::readText).orElse("")),
                find.findAllNow(GALLERY_IMAGES).size(),
                find.findNow(BOOK_BUTTON).isPresent());
    }

    public SearchResultsPage back() {
        tap(BACK);
        return new SearchResultsPage().waitForResults();
    }
}
