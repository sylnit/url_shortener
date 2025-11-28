package com.maliroso.url_shortener.service;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void resolveBucket_returnsSameBucketForSameKey() {
        String key = "user:123";
        long capacity = 5;
        Duration refillPeriod = Duration.ofSeconds(10);

        Bucket bucket1 = rateLimiterService.resolveBucket(key, capacity, refillPeriod);
        Bucket bucket2 = rateLimiterService.resolveBucket(key, capacity, refillPeriod);

        // Same key → same bucket instance (identity)
        assertSame(bucket1, bucket2);
    }

    @Test
    void resolveBucket_returnsDifferentBucketsForDifferentKeys() {
        long capacity = 3;
        Duration refillPeriod = Duration.ofMinutes(1);

        Bucket bucket1 = rateLimiterService.resolveBucket("key1", capacity, refillPeriod);
        Bucket bucket2 = rateLimiterService.resolveBucket("key2", capacity, refillPeriod);

        assertNotSame(bucket1, bucket2);
    }

    @Test
    void resolveBucket_createsBucketWithCorrectCapacity() {
        String key = "test";
        long capacity = 2;
        Duration refillPeriod = Duration.ofHours(1);

        Bucket bucket = rateLimiterService.resolveBucket(key, capacity, refillPeriod);

        // Should allow consuming up to 'capacity' tokens
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        // Now bucket is empty
        assertFalse(bucket.tryConsume(1));
    }

    @Test
    void resolveBucket_reusesExistingBucketEvenIfParamsChange() {
        String key = "fixed-key";
        Bucket bucket1 = rateLimiterService.resolveBucket(key, 1, Duration.ofSeconds(1));
        Bucket bucket2 = rateLimiterService.resolveBucket(key, 100, Duration.ofDays(1)); // different params

        // Should return the first bucket (key-based, params ignored after first creation)
        assertSame(bucket1, bucket2);

        // Verify it still has capacity = 1
        assertTrue(bucket1.tryConsume(1));
        assertFalse(bucket1.tryConsume(1)); // already empty
    }

    @Test
    void concurrentAccess_createsOnlyOneBucketPerKey() throws InterruptedException {
        String key = "concurrent-key";
        long capacity = 10;
        Duration refillPeriod = Duration.ofSeconds(5);

        // Simulate concurrent access
        Bucket[] buckets = new Bucket[10];
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                buckets[index] = rateLimiterService.resolveBucket(key, capacity, refillPeriod);
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // All should refer to the same bucket instance
        for (int i = 1; i < buckets.length; i++) {
            assertSame(buckets[0], buckets[i]);
        }
    }
}