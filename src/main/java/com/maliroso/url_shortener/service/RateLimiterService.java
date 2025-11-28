package com.maliroso.url_shortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, long capacity, Duration refillPeriod){
        return buckets.computeIfAbsent(key, k -> createBucket(capacity, refillPeriod));
    }

    private Bucket createBucket(long capacity, Duration refillPeriod){
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillPeriod)
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
