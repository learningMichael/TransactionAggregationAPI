package application;

import application.service.TransactionService;
import domain.model.Category;
import domain.model.Transaction;
import domain.port.out.TransactionRepositoryPort;
import domain.port.out.TransactionSourcePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TransactionService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock
    private TransactionSourcePort sourcePort;

    @Mock
    private TransactionRepositoryPort repositoryPort;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(List.of(sourcePort), repositoryPort);
    }

    @Test
    @DisplayName("aggregate() should categorize transactions using domain logic")
    void aggregate_shouldCategorizeTransactions() {
        String accountId = "ACC-001";
        Transaction uncategorized = new Transaction(
                UUID.randomUUID().toString(),
                accountId,
                new BigDecimal("250.00"),
                LocalDateTime.now(),
                "Woolworths Food",   // should map to GROCERIES
                null
        );
        when(sourcePort.fetchTransactions(accountId)).thenReturn(List.of(uncategorized));
        when(repositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Transaction> result = service.aggregate(accountId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo(Category.GROCERIES);
        verify(repositoryPort, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("aggregate() should return empty list when source returns nothing")
    void aggregate_shouldHandleEmptySource() {
        String accountId = "ACC-002";
        when(sourcePort.fetchTransactions(accountId)).thenReturn(List.of());

        List<Transaction> result = service.aggregate(accountId);

        assertThat(result).isEmpty();
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("aggregate() should not fail if one source throws an exception")
    void aggregate_shouldHandleSourceFailureGracefully() {
        String accountId = "ACC-003";
        when(sourcePort.fetchTransactions(accountId)).thenThrow(new RuntimeException("Bank API down"));

        List<Transaction> result = service.aggregate(accountId);

        assertThat(result).isEmpty();
        verify(repositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("aggregate() should persist each transaction once")
    void aggregate_shouldPersistAllTransactions() {
        String accountId = "ACC-004";
        List<Transaction> raw = List.of(
                makeTransaction(accountId, "Netflix monthly"),
                makeTransaction(accountId, "Eskom electricity"),
                makeTransaction(accountId, "Uber trip")
        );
        when(sourcePort.fetchTransactions(accountId)).thenReturn(raw);
        when(repositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.aggregate(accountId);

        verify(repositoryPort, times(3)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("getTransactions() should delegate to repository port")
    void getTransactions_shouldDelegateToRepository() {
        String accountId = "ACC-005";
        List<Transaction> stored = List.of(makeTransaction(accountId, "KFC order"));
        when(repositoryPort.findByAccountId(accountId)).thenReturn(stored);

        List<Transaction> result = service.getTransactions(accountId);

        assertThat(result).hasSize(1);
        verify(repositoryPort).findByAccountId(accountId);
    }

    // helpers

    private Transaction makeTransaction(String accountId, String description) {
        return new Transaction(
                UUID.randomUUID().toString(),
                accountId,
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                description,
                null
        );
    }
}
