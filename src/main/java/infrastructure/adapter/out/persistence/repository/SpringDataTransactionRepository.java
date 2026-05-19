package infrastructure.adapter.out.persistence.repository;

import domain.model.Category;
import infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, String> {

    List<TransactionEntity> findByAccountId(String accountId);

    List<TransactionEntity> findByAccountIdAndCategory(String accountId, Category category);
}
