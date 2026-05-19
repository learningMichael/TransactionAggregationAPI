package domain;

import domain.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Category#classify(String)}.
 *
 * <p>Tests every category with representative keywords and edge cases.
 * Using {@code @ParameterizedTest} with {@code @CsvSource} is a senior-level
 * pattern — one test method, many inputs, clean readability.</p>
 */
@DisplayName("Category.classify() Unit Tests")
class CategoryTest {

    @ParameterizedTest(name = "''{0}'' should classify as {1}")
    @CsvSource({
            // GROCERIES
            "Woolworths Food Court,      GROCERIES",
            "Checkers Hyper,             GROCERIES",
            "Pick n Pay Supermarket,     GROCERIES",
            "SPAR convenience store,     GROCERIES",
            // ENTERTAINMENT
            "Netflix subscription,       ENTERTAINMENT",
            "Spotify Premium,            ENTERTAINMENT",
            "Ster-Kinekor cinema,        ENTERTAINMENT",
            "DStv monthly,               ENTERTAINMENT",
            // UTILITIES
            "Eskom prepaid,              UTILITIES",
            "City of Cape Town water,    UTILITIES",
            "Telkom internet,            UTILITIES",
            "Municipality rates,         UTILITIES",
            // TRANSPORT
            "Uber trip to airport,       TRANSPORT",
            "Bolt ride,                  TRANSPORT",
            "Gautrain fare,              TRANSPORT",
            // FUEL
            "Engen petrol station,       FUEL",
            "Shell fuel,                 FUEL",
            "Caltex garage,              FUEL",
            // DINING
            "KFC meal,                   DINING",
            "McDonalds drive through,    DINING",
            "Nandos restaurant,          DINING",
            "Vida e Caffe coffee,        DINING",
            // HEALTH
            "Dischem pharmacy,           HEALTH",
            "Clicks healthcare,          HEALTH",
            "Dr Smith consultation,      HEALTH",
            // SHOPPING
            "Takealot online,            SHOPPING",
            "Amazon purchase,            SHOPPING",
            "H&M clothing store,         SHOPPING",
            // TRAVEL
            "FlySafair flight,           TRAVEL",
            "Airbnb booking,             TRAVEL",
            "OR Tambo airport parking,   TRAVEL",
            // OTHER
            "Unknown payment,            OTHER",
            ",                           OTHER"
    })
    @DisplayName("classify descriptions to correct categories")
    void classify(String description, String expectedCategory) {
        Category result = Category.classify(description == null || description.isBlank() ? null : description.trim());
        assertThat(result).isEqualTo(Category.valueOf(expectedCategory.trim()));
    }
}
