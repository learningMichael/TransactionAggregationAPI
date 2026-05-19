package infrastructure.adapter.out.cache;

import domain.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Cache adapter for transactions using Spring's {@link CacheManager} abstraction.
 *
 */
@Slf4j
@Component
public class TransactionCacheAdapter {

    private static final String CACHE_NAME = "transactions";

    private final CacheManager cacheManager;

    public TransactionCacheAdapter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Returns cached transactions for an account if present.
     *
     * @param accountId the account to look up
     * @return cached list, or empty on a cache miss
     */
    @SuppressWarnings("unchecked")
    public Optional<List<Transaction>> get(String accountId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache == null) return Optional.empty();
        Cache.ValueWrapper wrapper = cache.get(accountId);
        if (wrapper != null) {
            log.debug("Cache HIT for accountId={}", accountId);
            return Optional.ofNullable((List<Transaction>) wrapper.get());
        }
        log.debug("Cache MISS for accountId={}", accountId);
        return Optional.empty();
    }

    /**
     * Stores transactions in cache for the given account.
     *
     * @param accountId    the account key
     * @param transactions the transactions to cache
     */
    public void put(String accountId, List<Transaction> transactions) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.put(accountId, transactions);
            log.debug("Cached {} transactions for accountId={}", transactions.size(), accountId);
        }
    }

    /**
     * Evicts cached data for an account (called after fresh data is written).
     *
     * @param accountId the account to evict
     */
    public void evict(String accountId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(accountId);
            log.debug("Evicted cache for accountId={}", accountId);
        }
    }
}
