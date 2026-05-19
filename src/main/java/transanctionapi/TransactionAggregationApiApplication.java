package transanctionapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Entry point for the Transaction Aggregation API.
 * <p>{@code @EnableCaching} activates Spring's caching abstraction, backed by Redis
 * as configured in {@code application.yml}.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "transanctionapi",
        "domain",
        "application",
        "infrastructure"
})
@EnableCaching
@EntityScan(basePackages = "infrastructure.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "infrastructure.adapter.out.persistence.repository")
public class TransactionAggregationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionAggregationApiApplication.class, args);
    }
}
