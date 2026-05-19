package infrastructure.adapter.in.web;

import domain.model.Category;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Summary DTO showing total spend per category for an account.
 *
 * @param accountId       the account this summary belongs to
 * @param totalTransactions total number of transactions for this account
 * @param spendByCategory   map of category → net amount (negative = spend, positive = income)
 */
public record TransactionSummaryResponse(
        String accountId,
        int totalTransactions,
        Map<Category, BigDecimal> spendByCategory
) {}
