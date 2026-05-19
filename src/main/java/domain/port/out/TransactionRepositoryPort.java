package domain.port.out;

import domain.model.Category;
import domain.model.Transaction;

import java.util.List;

/**
 * Output port — defines what the application needs from the persistence layer.
 */
public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    List<Transaction> findAll();

    List<Transaction> findByAccountId(String accountId);

    List<Transaction> findByAccountIdAndCategory(String accountId, Category category);
}
