package com.example.NexSpend.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {

        return buckets.computeIfAbsent(
                ip,
                this::newBucket
        );
    }

    private Bucket newBucket(String ip) {

        Bandwidth limit =
                Bandwidth.simple(
                        20,
                        Duration.ofMinutes(1));

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
