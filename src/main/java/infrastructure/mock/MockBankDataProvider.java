package infrastructure.mock;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.model.Category;
import domain.model.Transaction;
import domain.port.out.TransactionSourcePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Mock implementation of {@link TransactionSourcePort}.
 *
 * <p>Reads transaction data from {@code src/main/resources/mock/transactions.json}.
 * The JSON contains {@code accountId}, {@code amount}, and {@code description} fields.
 * {@code id} and {@code timestamp} are generated at load time since the file omits them.</p>
 *
 * <p>Activated when {@code bank.api.mock.enabled=true} (the default).
 * Replaced in production by {@code BankApiAdapter} which calls the real bank HTTP endpoint.</p>
 *
 * <p>The service never knows which implementation it receives — this is the Ports and Adapters
 * pattern: the contract ({@link TransactionSourcePort}) is what the service depends on.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bank.api.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockBankDataProvider implements TransactionSourcePort {

    /** Classpath location of the mock transaction data file. */
    private static final String JSON_PATH = "mock/transactions.json";

    private final ObjectMapper objectMapper;

    /** All transactions loaded from JSON at construction time — read once, served many times. */
    private final List<JsonTransaction> allTransactions;

    public MockBankDataProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.allTransactions = loadFromClasspath();
        log.info("MockBankDataProvider loaded {} transactions from {}", allTransactions.size(), JSON_PATH);
    }

    /**
     * Returns all transactions from the JSON file that match the given {@code accountId}.
     *
     * <p>Because the JSON file does not contain {@code id} or {@code timestamp},
     * a random UUID is generated for each transaction and the timestamp is randomised
     * within the last 30 days to simulate realistic data spread.</p>
     *
     * <p>Category is intentionally left {@code null} — the {@code TransactionService}
     * calls {@code Category.classify()} after fetching, keeping categorization logic
     * inside the domain layer where it belongs.</p>
     *
     * @param accountId the account to filter transactions for
     * @return filtered, mapped domain transactions from the JSON file
     */
    @Override
    public List<Transaction> fetchTransactions(String accountId) {
        log.info("MockBankDataProvider: fetching transactions for accountId={}", accountId);

        List<Transaction> result = allTransactions.stream()
                .filter(t -> accountId.equals(t.accountId()))
                .map(t -> new Transaction(
                        UUID.randomUUID().toString(),
                        t.accountId(),
                        t.amount(),
                        randomTimestamp(),            // generate realistic timestamp
                        t.description(),
                        null                          // category assigned by service via Category.classify()
                ))
                .toList();

        log.info("MockBankDataProvider: returning {} transactions for accountId={}", result.size(), accountId);
        return result;
    }

    /**
     * Loads and deserializes the JSON array from the classpath at startup.
     * Returns an empty list if the file is missing or malformed so the app still starts.
     *
     * @return deserialized list of raw JSON transactions
     */
    private List<JsonTransaction> loadFromClasspath() {
        try {
            ClassPathResource resource = new ClassPathResource(JSON_PATH);
            JsonTransaction[] array = objectMapper.readValue(resource.getInputStream(), JsonTransaction[].class);
            return Arrays.asList(array);
        } catch (IOException e) {
            log.error("Failed to load mock transactions from {}: {}", JSON_PATH, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Generates a random timestamp within the last 30 days to simulate spread across a month.
     *
     * @return a {@link LocalDateTime} between now and 30 days ago
     */
    private LocalDateTime randomTimestamp() {
        long daysAgo = (long) (Math.random() * 30);
        long hoursAgo = (long) (Math.random() * 24);
        return LocalDateTime.now().minusDays(daysAgo).minusHours(hoursAgo);
    }

    /**
     * Internal record mapping the JSON structure from {@code transactions.json}.
     *
     * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} ensures the file can evolve
     * (extra fields added) without breaking deserialization.</p>
     *
     * @param accountId account this transaction belongs to
     * @param amount    monetary value of the transaction
     * @param description raw bank description text
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record JsonTransaction(String accountId, BigDecimal amount, String description) {}
}
