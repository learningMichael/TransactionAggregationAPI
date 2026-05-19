package domain.port.in;

import domain.model.Category;
import domain.model.Transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Input port — defines what actions the outside world (e.g. REST controller) can trigger.
 *
 * <p>The application service implements this interface.
 * The controller depends only on this contract, never on the service directly.</p>
 */
public interface AggregateTransactionUseCase {

    /** Fetches from all sources, categorizes, persists, and returns results. */
    List<Transaction> aggregate(String accountId);

    /** Retrieves all persisted transactions for an account. */
    List<Transaction> getTransactions(String accountId);

    /** Returns transactions for an account filtered by category. */
    List<Transaction> getByCategory(String accountId, Category category);

    /** Returns total spend per category for an account. */
    Map<Category, BigDecimal> getSummaryByCategory(String accountId);

    /** Returns the top N transactions by absolute amount (highest spend first). */
    List<Transaction> getTopTransactions(String accountId, int limit);

    /** Returns all transactions across all accounts. */
    List<Transaction> getAllTransactions();
}
