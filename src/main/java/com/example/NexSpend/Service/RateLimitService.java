package com.example.NexSpend.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private static final int MAX_BUCKETS = 10_000;
    private static final long ENTRY_TTL_MILLIS = Duration.ofMinutes(10).toMillis();

    private final Map<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {
        long now = System.currentTimeMillis();
        cleanupExpired(now);

        BucketEntry entry = buckets.computeIfAbsent(ip, key -> new BucketEntry(newBucket(), now));
        entry.lastAccess = now;

        // Keep memory bounded even if an attacker sends requests using many source IPs.
        if (buckets.size() > MAX_BUCKETS) {
            cleanupExpired(now);
            if (buckets.size() > MAX_BUCKETS) {
                String keyToRemove = buckets.keySet().stream().findFirst().orElse(null);
                if (keyToRemove != null) {
                    buckets.remove(keyToRemove);
                }
            }
        }

        return entry.bucket;
    }

    private void cleanupExpired(long now) {
        buckets.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccess > ENTRY_TTL_MILLIS);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.simple(20, Duration.ofMinutes(1));
        return Bucket.builder().addLimit(limit).build();
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile long lastAccess;

        private BucketEntry(Bucket bucket, long lastAccess) {
            this.bucket = bucket;
            this.lastAccess = lastAccess;
        }
    }
}
