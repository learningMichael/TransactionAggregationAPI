package infrastructure.adapter.out.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) representing a raw transaction received from an external bank API.
 *
 * @param id          raw transaction ID from the external bank
 * @param accountId   account this transaction belongs to
 * @param amount      monetary value
 * @param timestamp   when the transaction occurred
 * @param description raw description text (used for categorization)
 * @param category    raw category string from the bank (may be null or unrecognized)
 */
public record ExternalTransactionDto(
        String id,
        String accountId,
        BigDecimal amount,
        LocalDateTime timestamp,
        String description,
        String category
) {}
