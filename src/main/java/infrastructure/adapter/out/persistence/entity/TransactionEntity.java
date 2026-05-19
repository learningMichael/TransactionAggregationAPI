package infrastructure.adapter.out.persistence.entity;

import domain.model.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing the {@code transactions} database table.
 *
 */
@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {

    /** Primary key — UUID stored as a string. */
    @Id
    private String id;

    /** The customer account this transaction belongs to. */
    @Column(name = "account_id", nullable = false)
    private String accountId;

    /** Transaction monetary value. Stored as NUMERIC(19,4) for precision. */
    @Column(nullable = false)
    private BigDecimal amount;

    /** The exact date/time this transaction occurred. */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Human-readable description from the originating bank. */
    @Column(length = 255)
    private String description;

    /** Categorized value stored as its string name (not ordinal) for readability. */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Category category;
}
