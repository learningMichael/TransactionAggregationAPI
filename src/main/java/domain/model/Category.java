package domain.model;

/**
 * Enumeration of transaction categories.
 *
 * <p>
 * Contains the core classification logic — the domain decides how to categorize
 * a transaction based on its description. This keeps business rules inside the domain,
 * not in the service or infrastructure layers.
 * </p>
 */
public enum Category {

    GROCERIES,
    FUEL,
    DINING,
    ENTERTAINMENT,
    TRANSPORT,
    UTILITIES,
    HEALTH,
    SHOPPING,
    TRAVEL,
    OTHER;

    /**
     * Classifies a transaction based on its description using keyword matching.
     *
     * @param description the raw transaction description from the bank
     * @return the most appropriate {@link Category} for the description
     */
    public static Category classify(String description) {
        if (description == null || description.isBlank()) return OTHER;

        String lower = description.toLowerCase();

        if (lower.contains("checkers") || lower.contains("woolworths") ||
            lower.contains("pick n pay") || lower.contains("shoprite") ||
            lower.contains("spar") || lower.contains("grocery")) {
            return GROCERIES;
        }
        if (lower.contains("netflix") || lower.contains("spotify") ||
            lower.contains("cinema") || lower.contains("showmax") ||
            lower.contains("dstv") || lower.contains("gaming")) {
            return ENTERTAINMENT;
        }
        if (lower.contains("eskom") || lower.contains("water") ||
            lower.contains("electricity") || lower.contains("municipality") ||
            lower.contains("rates") || lower.contains("internet") ||
            lower.contains("telkom")) {
            return UTILITIES;
        }
        if (lower.contains("uber") || lower.contains("bolt") ||
            lower.contains("taxi") || lower.contains("bus") ||
            lower.contains("train") || lower.contains("gautrain")) {
            return TRANSPORT;
        }
        if (lower.contains("petrol") || lower.contains("fuel") ||
            lower.contains("engen") || lower.contains("shell") ||
            lower.contains("bp ") || lower.contains("caltex")) {
            return FUEL;
        }
        if (lower.contains("restaurant") || lower.contains("kfc") ||
            lower.contains("mcdonald") || lower.contains("steers") ||
            lower.contains("nandos") || lower.contains("cafe") ||
            lower.contains("coffee") || lower.contains("food")) {
            return DINING;
        }
        if (lower.contains("pharmacy") || lower.contains("hospital") ||
            lower.contains("doctor") || lower.contains("clinic") ||
            lower.contains("dischem") || lower.contains("clicks") ||
            lower.contains("dr ") || lower.contains("dr.")) {
            return HEALTH;
        }
        if (lower.contains("takealot") || lower.contains("amazon") ||
            lower.contains("clothing") || lower.contains("zara") ||
            lower.contains("h&m") || lower.contains("mall")) {
            return SHOPPING;
        }
        if (lower.contains("flight") || lower.contains("hotel") ||
            lower.contains("airbnb") || lower.contains("booking") ||
            lower.contains("travel") || lower.contains("airport")) {
            return TRAVEL;
        }

        return OTHER;
    }
}
