package infrastructure.adapter.in.web;

import domain.model.Category;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO returned to the API consumer.
 *
 * @param id          unique transaction identifier
 * @param accountId   account this transaction belongs to
 * @param amount      monetary value of the transaction
 * @param timestamp   when the transaction occurred
 * @param description human-readable description
 * @param category    classified category (e.g. GROCERIES, FUEL)
 */
public record TransactionResponse(
        String id,
        String accountId,
        BigDecimal amount,
        LocalDateTime timestamp,
        String description,
        Category category
) {}
