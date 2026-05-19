package infrastructure.adapter.in.web;

import domain.model.Category;
import domain.model.Transaction;
import domain.port.in.AggregateTransactionUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller — the primary input adapter.

 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final AggregateTransactionUseCase useCase;

    /**
     * POST /api/v1/transactions/aggregate/{accountId}
     * Fetches from all mock bank sources, categorizes, persists, and returns results.
     */
    @PostMapping("/aggregate/{accountId}")
    public ResponseEntity<List<TransactionResponse>> aggregate(
            @PathVariable @NotBlank String accountId) {
        log.info("POST /aggregate/{}", accountId);
        return ResponseEntity.ok(
                useCase.aggregate(accountId).stream().map(this::toResponse).toList()
        );
    }

    /**
     * GET /api/v1/transactions/{accountId}
     * Returns all persisted (and cached) transactions for an account.
     */
    @GetMapping("/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable @NotBlank String accountId) {
        log.info("GET /transactions/{}", accountId);
        return ResponseEntity.ok(
                useCase.getTransactions(accountId).stream().map(this::toResponse).toList()
        );
    }

    /**
     * GET /api/v1/transactions/{accountId}/category/{category}
     * Returns transactions for an account filtered by a specific category.
     * Example: GET /api/v1/transactions/ACC-123456/category/GROCERIES
     */
    @GetMapping("/{accountId}/category/{category}")
    public ResponseEntity<List<TransactionResponse>> getByCategory(
            @PathVariable @NotBlank String accountId,
            @PathVariable Category category) {
        log.info("GET /transactions/{}/category/{}", accountId, category);
        return ResponseEntity.ok(
                useCase.getByCategory(accountId, category).stream().map(this::toResponse).toList()
        );
    }

    /**
     * GET /api/v1/transactions/{accountId}/summary
     * Returns total spend per category for an account.
     * Useful for budgeting dashboards and spend analysis.
     */
    @GetMapping("/{accountId}/summary")
    public ResponseEntity<TransactionSummaryResponse> getSummary(
            @PathVariable @NotBlank String accountId) {
        log.info("GET /transactions/{}/summary", accountId);
        var summary = useCase.getSummaryByCategory(accountId);
        var total = useCase.getTransactions(accountId).size();
        return ResponseEntity.ok(new TransactionSummaryResponse(accountId, total, summary));
    }

    /**
     * GET /api/v1/transactions/{accountId}/top?limit=10
     * Returns the top N transactions by absolute amount (highest spend first).
     * Defaults to top 10 if limit is not specified.
     */
    @GetMapping("/{accountId}/top")
    public ResponseEntity<List<TransactionResponse>> getTopTransactions(
            @PathVariable @NotBlank String accountId,
            @RequestParam(defaultValue = "10") @Positive int limit) {
        log.info("GET /transactions/{}/top?limit={}", accountId, limit);
        return ResponseEntity.ok(
                useCase.getTopTransactions(accountId, limit).stream().map(this::toResponse).toList()
        );
    }

    /**
     * GET /api/v1/transactions
     * Returns all transactions across all accounts.
     * Useful for admin/reporting views.
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions() {
        log.info("GET /transactions (all)");
        return ResponseEntity.ok(
                useCase.getAllTransactions().stream().map(this::toResponse).toList()
        );
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.id(),
                t.accountId(),
                t.amount(),
                t.timestamp(),
                t.description(),
                t.category()
        );
    }
}
