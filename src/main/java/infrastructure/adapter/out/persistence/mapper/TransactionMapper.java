package infrastructure.adapter.out.persistence.mapper;

import domain.model.Transaction;
import infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper that converts between the domain {@link Transaction} record
 * and the JPA {@link TransactionEntity}.
 *
 * <p>MapStruct generates this implementation at compile time — no reflection,
 * no runtime overhead. Field names match exactly, so no explicit {@code @Mapping}
 * annotations are needed here.</p>
 *
 * <p>{@code componentModel = "spring"} registers the generated implementation
 * as a Spring bean, allowing it to be injected with {@code @Autowired} or
 * constructor injection.</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TransactionMapper {

    /**
     * Converts a JPA entity to a domain record.
     *
     * @param entity the entity retrieved from the database
     * @return a pure domain {@link Transaction} record
     */
    Transaction toDomain(TransactionEntity entity);

    /**
     * Converts a domain record to a JPA entity ready for persistence.
     *
     * @param transaction the domain transaction
     * @return a {@link TransactionEntity} with all fields mapped
     */
    TransactionEntity toEntity(Transaction transaction);
}
