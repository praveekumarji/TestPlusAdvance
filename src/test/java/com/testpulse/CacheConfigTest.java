package com.testpulse;

import com.github.benmanes.caffeine.cache.Cache;
import com.testpulse.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheConfigTest {

    @Test
    void shouldConfigureRequestedCacheExpirations() {
        CacheManager cacheManager = new CacheConfig().cacheManager();

        assertNoExpiry(cacheManager, "tests");
        assertNoExpiry(cacheManager, "questions");
        assertNoExpiry(cacheManager, "educationClasses");
        assertExpiry(cacheManager, "subscriptionPlans", TimeUnit.DAYS.toMillis(7));
        assertExpiry(cacheManager, "coupons", TimeUnit.DAYS.toMillis(7));
        assertExpiry(cacheManager, "referralStatuses", TimeUnit.MINUTES.toMillis(10));
        assertExpiry(cacheManager, "users", TimeUnit.MINUTES.toMillis(10));
    }

    private void assertNoExpiry(CacheManager cacheManager, String name) {
        Cache<Object, Object> cache = nativeCache(cacheManager, name);
        assertTrue(cache.policy().expireAfterWrite().isEmpty(), name + " should not expire by time");
    }

    private void assertExpiry(CacheManager cacheManager, String name, long expectedMillis) {
        Cache<Object, Object> cache = nativeCache(cacheManager, name);
        long actualMillis = cache.policy().expireAfterWrite().orElseThrow()
                .getExpiresAfter(TimeUnit.MILLISECONDS);
        assertEquals(expectedMillis, actualMillis, name + " should use the configured expiry");
    }

    private Cache<Object, Object> nativeCache(CacheManager cacheManager, String name) {
        return ((CaffeineCache) cacheManager.getCache(name)).getNativeCache();
    }
}