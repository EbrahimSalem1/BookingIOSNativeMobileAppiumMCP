package com.booking.automation.unit;

import com.booking.automation.config.FrameworkConfig;
import com.booking.automation.constants.ConfigKeys;
import com.booking.automation.data.TestDataRepository;
import com.booking.automation.models.FilterCriteria;
import com.booking.automation.models.SearchCriteria;
import com.booking.automation.models.SortOption;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Catches broken YAML / JSON before a 20-minute device run does. */
public class ConfigAndTestDataTest {

    private final TestDataRepository data = TestDataRepository.get();

    @Test
    public void configurationLoadsWithSaneDefaults() {
        FrameworkConfig config = FrameworkConfig.get();
        assertThat(config.getString(ConfigKeys.DEVICE_POOL)).isNotBlank();
        assertThat(config.getString(ConfigKeys.DEVICE_PLATFORM_VERSION)).matches("\\d+(\\.\\d+)*");
        assertThat(config.getSeconds(ConfigKeys.WAIT_EXPLICIT, 0)).isPositive();
        assertThat(config.getSeconds(ConfigKeys.WAIT_LONG, 0)).isGreaterThan(config.getSeconds(ConfigKeys.WAIT_EXPLICIT, 0));
    }

    @Test
    public void secretsAreMaskedInReportSnapshot() {
        assertThat(FrameworkConfig.get().safeSnapshot())
                .allSatisfy((key, value) -> {
                    if (key.toLowerCase().contains("password") || key.toLowerCase().contains("accesskey")) {
                        assertThat(value).isEqualTo("******");
                    }
                });
    }

    @Test
    public void everySearchDatasetIsValidAndNeverExpires() {
        for (String alias : new String[]{"paris_weekend", "london_business_trip", "cairo_family", "no_results"}) {
            SearchCriteria criteria = data.search(alias);
            assertThat(criteria.checkIn()).as(alias + " check-in").isAfterOrEqualTo(LocalDate.now());
            assertThat(criteria.relevanceKeyword()).as(alias + " relevance keyword").isNotBlank();
        }
        assertThat(data.defaultSearch().destination()).isNotBlank();
    }

    @Test
    public void filtersHaveExecutableRules() {
        FilterCriteria filter = data.defaultFilter();
        assertThat(filter.uiLabel()).isNotBlank();
        assertThat(filter.rule()).isNotNull();
    }

    @Test
    public void everySortOptionHasAUiLabel() {
        for (SortOption option : SortOption.values()) {
            assertThat(data.sortLabel(option)).as(option.name()).isNotBlank();
        }
    }

    @Test
    public void unknownAliasFailsWithAHelpfulMessage() {
        assertThatThrownBy(() -> data.search("atlantis"))
                .isInstanceOf(TestDataRepository.TestDataException.class)
                .hasMessageContaining("Available: paris_weekend");
    }

    @Test
    public void nonSecretCredentialsResolve() {
        assertThat(data.credentials("unregistered_user").username()).contains("@");
        assertThat(data.credentials("unregistered_user").toString()).doesNotContain("Any-Passw0rd!");
    }
}
