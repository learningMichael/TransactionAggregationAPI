package infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configuration for the Spring {@link RestClient} used by {@code BankApiAdapter}.
 */
@Configuration
public class RestClientConfig {

    /** Base URL of the external bank API — set via {@code bank.api.base-url} in application.yml. */
    @Value("${bank.api.base-url:http://localhost:9090}")
    private String baseUrl;

    /** Connection timeout in milliseconds — set via {@code bank.api.connect-timeout-ms}. */
    @Value("${bank.api.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    /** Read timeout in milliseconds — set via {@code bank.api.read-timeout-ms}. */
    @Value("${bank.api.read-timeout-ms:5000}")
    private int readTimeoutMs;

    /**
     * Creates and configures the {@link RestClient} bean.
     *
     * <p>A single shared RestClient is thread-safe and should be reused — not recreated per request.</p>
     *
     * @return configured {@link RestClient} ready for injection
     */
    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
