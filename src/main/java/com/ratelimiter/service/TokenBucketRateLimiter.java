package com.ratelimiter.service;

import com.ratelimiter.annotations.Restrict;
import com.ratelimiter.model.data.Bucket;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBucketRateLimiter implements RateLimiter{
    private  Bucket bucket;
    private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<String, Double> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedClients = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRefillTimes = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(double capacity, double refillRate, double refillTimeMillis, long blockDurationMillis, int blockThreshold) {
        this.bucket = Bucket.builder()
                .capacity(capacity)
                .refillRate(refillRate)
                .refillTimeMillis(refillTimeMillis)
                .blockDurationMillis(blockDurationMillis)
                .blockThreshold(blockThreshold)
                .build();
    }


    /**
     * Checks if a client is allowed to make a request based on the rate limit settings.
     *
     * @param clientId the client identifier
     * @return true if the client is allowed to make a request, false otherwise
     */
    @Override
    public synchronized boolean allow(String clientId) {
        long currentTime = System.currentTimeMillis();

        if (isBlocked(clientId, currentTime)) return false;

        if (isBlockThresholdExceeded(clientId)) {
            blockClient(clientId, currentTime);
            return false;
        }

        if (isRateExceeded(clientId, currentTime)) {
            incrementViolationCount(clientId);
            return false;
        }

        refillTokens(clientId, currentTime);

        double tokenCount = tokens.getOrDefault(clientId, 0.0);

        if (tokenCount >= 1) {
            tokens.put(clientId, tokenCount - 1);
            return true;
        } else {
            lastRequestTimes.put(clientId, currentTime);
            return false;
        }
    }

    protected boolean isBlockThresholdExceeded(String clientId) {
        Integer violationCount = violationCounts.getOrDefault(clientId, 0);
        return violationCount >= bucket.getBlockThreshold();
    }


    protected boolean isBlocked(String clientId, long currentTime) {
        Long blockedUntil = blockedClients.get(clientId);
        if (blockedUntil != null) {
            if (blockedUntil <= currentTime) {
                blockedClients.remove(clientId);
                violationCounts.remove(clientId);
            } else {
                return true;
            }
        }
        return false;
    }

    protected void blockClient(String clientId, long currentTime) {
        long blockedUntil = currentTime + bucket.getBlockDurationMillis();
        blockedClients.put(clientId, blockedUntil);
    }

    private boolean isRateExceeded(String clientId, long currentTime) {
        Long lastRequestTime = lastRequestTimes.get(clientId);
        if (lastRequestTime != null) {
            long timeElapsed = currentTime - lastRequestTime;
            double tokenCount = tokens.getOrDefault(clientId, 0.0);

            if (tokenCount < 1 && timeElapsed <= bucket.getRefillRate()) {
                return true;
            }
        }
        lastRequestTimes.put(clientId, currentTime);
        return false;
    }

    protected void incrementViolationCount(String clientId) {
        violationCounts.compute(clientId, (key, value) -> value == null ? 1 : value + 1);
    }

    private void refillTokens(String clientId, long currentTime) {
        tokens.compute(clientId, (key, value) -> {
            if(value == null) {
                return bucket.getCapacity();
            }
            double currentTokens = tokens.getOrDefault(clientId, 0.0);
            long timeElapsed = currentTime - lastRefillTimes.getOrDefault(clientId, currentTime);

            double tokensToAdd = bucket.getRefillRate() * (timeElapsed / bucket.getRefillTimeMillis());
            double newTokens = currentTokens + tokensToAdd;

            double token = Math.min(newTokens, bucket.getCapacity());
            token = Math.max(token, 0.0);
            lastRefillTimes.put(clientId, currentTime);
            return token;
        });
    }


    /**
     * Calculates the remaining time in seconds until the rate limit allows the next request for the given client.
     *
     * @param clientId the client identifier
     * @return the remaining time in seconds until the next request is allowed, or 0 if the client can make a request immediately
     */
    @Override
    public long getRemainingTimeSec(String clientId) {
        long currentTime = System.currentTimeMillis();
        Long lastRequestTime = lastRequestTimes.get(clientId);
        Long blockedTime = blockedClients.getOrDefault(clientId, lastRequestTime);

        if (lastRequestTime == null) {
            return 0;
        } else {
            long timeElapsed = currentTime - blockedTime;
            long remainingTime = (long) (bucket.getRefillTimeMillis() - (timeElapsed % bucket.getRefillTimeMillis()));
            return remainingTime / 1000;
        }
    }


    private Bucket createDefaultBucket(Restrict annotation) {
        double capacity = annotation.capacity();
        double refillRate = annotation.refillRate();
        double refillTimeMillis = annotation.refillTimeMillis();
        long blockDurationMillis = annotation.blockDurationMillis();
        int blockThreshold = annotation.blockThreshold();
        return Bucket.builder()
                .capacity(capacity)
                .refillTimeMillis(refillTimeMillis)
                .refillRate(refillRate)
                .blockThreshold(blockThreshold)
                .blockDurationMillis(blockDurationMillis)
                .build();

    }
}
