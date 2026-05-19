package domain.port.out;

import domain.model.Transaction;

import java.util.List;

/**
 * Output port — defines what the application needs from an external data source.
 *
 * <p>The infrastructure layer implements this interface (e.g. BankApiAdapter using RestClient).
 * The application layer depends only on this contract, never on the HTTP implementation.</p>
 */
public interface TransactionSourcePort {

    /**
     * Fetches raw transactions from an external bank data source.
     *
     * @param accountId the account to retrieve transactions for
     * @return list of transactions fetched from the external source
     */
    List<Transaction> fetchTransactions(String accountId);
}
