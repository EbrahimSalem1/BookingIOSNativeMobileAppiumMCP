package com.booking.automation.unit;

import com.booking.automation.utils.TextParsers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class TextParsersTest {

    @DataProvider
    public Object[][] prices() {
        return new Object[][]{
                {"$120", "120"},
                {"$1,250.50", "1250.50"},
                {"EGP 2,450 / night", "2450"},
                {"€ 99", "99"},
                {"From £1 200 total", "1200"},
                {"US$85.99 per night", "85.99"},
        };
    }

    @Test(dataProvider = "prices")
    public void parsesDisplayedPrices(String text, String expected) {
        assertThat(TextParsers.price(text)).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo(new BigDecimal(expected)));
    }

    @Test
    public void emptyOrPricelessTextHasNoPrice() {
        assertThat(TextParsers.price("")).isEmpty();
        assertThat(TextParsers.price("Sold out")).isEmpty();
        assertThat(TextParsers.price(null)).isEmpty();
    }

    @DataProvider
    public Object[][] ratings() {
        return new Object[][]{
                {"4.5", 4.5},
                {"8.6/10", 4.3},
                {"Rated 4,2 out of 5", 4.2},
                {"9 / 10", 4.5},
        };
    }

    @Test(dataProvider = "ratings")
    public void normalisesRatingsToFivePointScale(String text, double expected) {
        assertThat(TextParsers.rating(text)).contains(expected);
    }

    @Test
    public void relevanceMatchIgnoresCaseAndWhitespace() {
        assertThat(TextParsers.containsIgnoringCase("  8th arr.,   PARIS ", "paris")).isTrue();
        assertThat(TextParsers.containsIgnoringCase("Lyon", "Paris")).isFalse();
    }
}
