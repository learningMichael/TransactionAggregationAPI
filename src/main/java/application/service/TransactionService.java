package application.service;

import domain.model.Category;
import domain.model.Transaction;
import domain.port.in.AggregateTransactionUseCase;
import domain.port.out.TransactionRepositoryPort;
import domain.port.out.TransactionSourcePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Core application service that orchestrates transaction aggregation.
 *
 * <p>This service implements the {@link AggregateTransactionUseCase} input port.
 * It depends only on abstractions (ports), never on infrastructure details.</p>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService implements AggregateTransactionUseCase {

    /** All registered bank API adapters — injected as a list so new sources need zero service changes. */
    private final List<TransactionSourcePort> sources;

    /** Persistence port — the service never imports JPA or Spring Data. */
    private final TransactionRepositoryPort repositoryPort;

    /**
     * Fetches transactions from all registered sources in parallel using Java 21 Virtual Threads,
     * categorizes each transaction, persists them, and returns the result.
     *
     * <p>If a single source fails, the others are not affected — we collect partial results.</p>
     *
     * @param accountId the account to aggregate transactions for
     * @return deduplicated, categorized, and persisted transactions
     */
    @Override
    @CacheEvict(value = "transactions", key = "#accountId")
    public List<Transaction> aggregate(String accountId) {
        log.info("Starting parallel aggregation for accountId={}", accountId);

        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            List<CompletableFuture<List<Transaction>>> futures = sources.stream()
                    .map(source -> CompletableFuture.supplyAsync(
                            () -> fetchSafely(source, accountId),
                            virtualThreadExecutor
                    ))
                    .toList();

            // Wait for ALL sources to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            List<Transaction> allRaw = futures.stream()
                    .flatMap(f -> f.join().stream())
                    .toList();

            log.info("Fetched {} raw transactions from {} sources", allRaw.size(), sources.size());

            // Categorize each transaction
            List<Transaction> categorized = allRaw.stream()
                    .map(t -> new Transaction(
                            t.id(),
                            t.accountId(),
                            t.amount(),
                            t.timestamp(),
                            t.description(),
                            Category.classify(t.description())   // domain decides the category
                    ))
                    .toList();

            // Persist each categorized transaction
            categorized.forEach(repositoryPort::save);

            log.info("Persisted {} categorized transactions for accountId={}", categorized.size(), accountId);
            return categorized;
        }
    }

    /**
     * Returns stored transactions for an account, with Redis caching.
     * Redis is checked first; DB is only hit on a cache miss.
     *
     * @param accountId the account to retrieve
     * @return cached or freshly loaded transactions
     */
    @Override
    @Cacheable(value = "transactions", key = "#accountId")
    public List<Transaction> getTransactions(String accountId) {
        log.info("Loading transactions from DB for accountId={}", accountId);
        return repositoryPort.findByAccountId(accountId);
    }

    @Override
    public List<Transaction> getByCategory(String accountId, Category category) {
        log.info("Loading transactions for accountId={} category={}", accountId, category);
        return repositoryPort.findByAccountIdAndCategory(accountId, category);
    }

    @Override
    public Map<Category, BigDecimal> getSummaryByCategory(String accountId) {
        log.info("Building category summary for accountId={}", accountId);
        return repositoryPort.findByAccountId(accountId).stream()
                .filter(t -> t.category() != null)
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ));
    }

    @Override
    public List<Transaction> getTopTransactions(String accountId, int limit) {
        log.info("Fetching top {} transactions for accountId={}", limit, accountId);
        return repositoryPort.findByAccountId(accountId).stream()
                .sorted(Comparator.comparing(t -> t.amount().abs(), Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    @Override
    public List<Transaction> getAllTransactions() {
        log.info("Fetching all transactions");
        return repositoryPort.findAll();
    }

    private List<Transaction> fetchSafely(TransactionSourcePort source, String accountId) {
        try {
            return source.fetchTransactions(accountId);
        } catch (Exception ex) {
            log.warn("Source {} failed for accountId={}: {}", source.getClass().getSimpleName(), accountId, ex.getMessage());
            return Collections.emptyList();
        }
    }
}
