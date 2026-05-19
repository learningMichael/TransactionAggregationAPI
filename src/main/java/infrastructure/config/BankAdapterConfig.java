package infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the external bank API adapter.
 *
 * <p>All values injected from {@code application.yml}.
 */
@Configuration
public class BankAdapterConfig {

    /** Base URL of the bank API, overridable via the {@code BANK_API_BASE_URL} environment variable. */
    @Value("${bank.api.base-url:http://localhost:9090}")
    private String baseUrl;

    /** Optional API key for authenticated bank endpoints. */
    @Value("${bank.api.key:}")
    private String apiKey;

    public String getBaseUrl() { return baseUrl; }
    public String getApiKey()  { return apiKey;  }
}
