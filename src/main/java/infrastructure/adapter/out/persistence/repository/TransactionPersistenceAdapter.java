package infrastructure.adapter.out.persistence.repository;

import domain.model.Category;
import domain.model.Transaction;
import domain.port.out.TransactionRepositoryPort;
import infrastructure.adapter.out.persistence.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final SpringDataTransactionRepository jpaRepository;
    private final TransactionMapper mapper;

    public TransactionPersistenceAdapter(
            SpringDataTransactionRepository jpaRepository,
            @Qualifier("transactionMapperImpl") TransactionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    /**
     * Saves a domain transaction by converting it to a JPA entity first.
     *
     * @param transaction the domain transaction to persist
     * @return the saved transaction mapped back to a domain object
     */
    @Override
    public Transaction save(Transaction transaction) {
        log.debug("Persisting transaction id={}", transaction.id());
        var entity = mapper.toEntity(transaction);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    /**
     * Retrieves all stored transactions and maps them back to domain objects.
     *
     * @return all transactions as domain records
     */
    @Override
    public List<Transaction> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * Retrieves all transactions for a specific account.
     *
     * @param accountId the account to query
     * @return domain transactions for this account
     */
    @Override
    public List<Transaction> findByAccountId(String accountId) {
        log.debug("Loading transactions for accountId={}", accountId);
        return jpaRepository.findByAccountId(accountId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findByAccountIdAndCategory(String accountId, Category category) {
        log.debug("Loading transactions for accountId={} category={}", accountId, category);
        return jpaRepository.findByAccountIdAndCategory(accountId, category)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
