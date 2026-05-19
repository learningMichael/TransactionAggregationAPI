package infrastructure.adapter.out.client;

import domain.model.Category;
import domain.model.Transaction;
import domain.port.out.TransactionSourcePort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Bank API adapter — implements {@link TransactionSourcePort} using Spring {@link RestClient}.
 *
 * <ul>
 *   <li>{@link RestClient} — fluent, native Spring HTTP client (replaces RestTemplate and WebClient for sync use cases).</li>
 *   <li>{@code @CircuitBreaker} — if the bank API fails repeatedly, the circuit opens and
 *       {@link #fallback} is called immediately, preventing thread starvation.</li>
 *   <li>{@code @Retry} — automatically retries transient failures (e.g. 500s, timeouts)
 *       before the circuit breaker counts them as failures.</li>
 *   <li>Fallback returns an empty list — partial results are better than a full 500 response.</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bank.api.mock.enabled", havingValue = "false")
@RequiredArgsConstructor
public class BankApiAdapter implements TransactionSourcePort {

    /** Injected RestClient — pre-configured with base URL, timeouts, and headers in {@code RestClientConfig}. */
    private final RestClient restClient;

    /**
     * @param accountId the account to fetch transactions for
     * @return list of domain {@link Transaction} objects
     */
    @Override
    @CircuitBreaker(name = "bankApi", fallbackMethod = "fallback")
    @Retry(name = "bankApi")
    public List<Transaction> fetchTransactions(String accountId) {
        log.info("Calling external bank API for accountId={}", accountId);

        List<ExternalTransactionDto> dtos = restClient.get()
                .uri("/transactions/{accountId}", accountId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (dtos == null || dtos.isEmpty()) {
            log.warn("No transactions returned from bank API for accountId={}", accountId);
            return Collections.emptyList();
        }

        return dtos.stream()
                .map(dto -> new Transaction(
                        dto.id() != null ? dto.id() : UUID.randomUUID().toString(),
                        dto.accountId() != null ? dto.accountId() : accountId,
                        dto.amount(),
                        dto.timestamp(),
                        dto.description(),
                        Category.classify(dto.description())   // domain classifies, not the adapter
                ))
                .toList();
    }

    /**
     * Fallback method invoked when the circuit is open or all retries are exhausted.
     * @param accountId the account that was being fetched
     * @param ex        the exception that triggered the fallback
     * @return empty list
     */
    public List<Transaction> fallback(String accountId, Exception ex) {
        log.warn("Circuit breaker / retry exhausted for accountId={}. Reason: {}. Returning empty list.",
                accountId, ex.getMessage());
        return Collections.emptyList();
    }
}
