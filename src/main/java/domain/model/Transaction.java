package domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Core domain model representing a single financial transaction.
 *
 * @param id          unique identifier for this transaction (UUID)
 * @param accountId   the customer account this transaction belongs to
 * @param amount      the monetary value (positive = credit, negative = debit)
 * @param timestamp   the exact date and time the transaction occurred
 * @param description the human-readable description from the bank
 * @param category    the classified category, determined by {@link Category#classify(String)}
 */
public record Transaction(
        String id,
        String accountId,
        BigDecimal amount,
        LocalDateTime timestamp,
        String description,
        Category category
) {}
