package com.ratelimiter.service;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@NoArgsConstructor
public class TokenBucketRateLimiter implements RateLimiter{
    @Value("${rate-limiter.capacity:1000}")
    private double capacity;

    @Value("${rate-limiter.refill-rate:100}")
    private double refillRate;

    @Value("${rate-limiter.block-duration-millis:60000}")
    private long blockDurationMillis;

    @Value("${rate-limiter.block-threshold:10}")
    private int blockThreshold;

    public TokenBucketRateLimiter(double capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    public TokenBucketRateLimiter(double capacity, double refillRate, long blockDurationMillis, int blockThreshold) {
        this(capacity, refillRate);
        this.blockDurationMillis = blockDurationMillis;
        this.blockThreshold = blockThreshold;
    }

    private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<String, Double> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedClients = new ConcurrentHashMap<>();


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
            // Block the client for the specified duration
            blockClient(clientId, currentTime);
            return false;
        }

        if (isRateExceeded(clientId, currentTime)) {
            incrementViolationCount(clientId);
            return false;
        }
        double tokenCount = updateTokenCount(clientId, currentTime);

        // Check if there are enough tokens for the request
        if (tokenCount >= 1) {
            tokens.put(clientId, tokenCount - 1);
            return true;
        } else {
            // Update the last request time
            lastRequestTimes.put(clientId, currentTime);
            return false;
        }
    }

    protected boolean isBlocked(String clientId, long currentTime) {
        Long blockedUntil = blockedClients.get(clientId);
        if (blockedUntil != null) {
            if (blockedUntil <= currentTime) {
                // Remove the client from the block map
                blockedClients.remove(clientId);
                // Reset the violation count
                violationCounts.remove(clientId);
            } else {
                // Client is still blocked
                return true;
            }
        }
        return false;
    }

    protected void blockClient(String clientId, long currentTime) {
        long blockedUntil = currentTime + blockDurationMillis;
        blockedClients.put(clientId, blockedUntil);
    }

    private boolean isRateExceeded(String clientId, long currentTime) {
        Long lastRequestTime = lastRequestTimes.get(clientId);
        if (lastRequestTime != null && currentTime - lastRequestTime <= refillRate) {
            // Client has exceeded the rate limit
            return true;
        } else {
            // Update the last request time
            lastRequestTimes.put(clientId, currentTime);
            return false;
        }
    }

    private boolean isBlockThresholdExceeded(String clientId) {
        Integer violationCount = violationCounts.get(clientId);
        return violationCount != null && violationCount >= blockThreshold;
    }

    protected void incrementViolationCount(String clientId) {
        violationCounts.compute(clientId, (key, value) -> value == null ? 1 : value + 1);
    }

    private double updateTokenCount(String clientId, long currentTime) {
        return tokens.compute(clientId, (key, value) -> {
            if (value == null) {
                return capacity;
            } else {
                double elapsedTime = (currentTime - System.nanoTime()) / 1e9;
                double newTokens = value + refillRate * elapsedTime;
                return Math.min(newTokens, capacity);
            }
        });
    }


    /**
     * Calculates the remaining time in milliseconds until the rate limit allows the next request for the given client.
     *
     * @param clientId the client identifier
     * @return the remaining time in milliseconds until the next request is allowed, or 0 if the client can make a request immediately
     */
    @Override
    public long getRemainingTimeMillis(String clientId) {
        double tokenCount = tokens.compute(clientId, (key, value) -> {
            if (value == null) {
                return capacity;
            } else {
                double newTokens = value + refillRate * (System.currentTimeMillis() - System.nanoTime()) / 1e9;
                return Math.min(newTokens, capacity);
            }
        });

        if (tokenCount >= 1) {
            // The client can make a request immediately
            return 0;
        } else {
            // Calculate the remaining time until the next request is allowed
            double remainingTimeSeconds = tokenCount / refillRate;
            return (long) (remainingTimeSeconds * 1000);
        }
    }


    public int getViolationCount(String clientId) {
        return violationCounts.get(clientId) == null ? 0 : violationCounts.get(clientId);
    }
}
