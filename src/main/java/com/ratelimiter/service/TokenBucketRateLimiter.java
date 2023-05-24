package com.ratelimiter.service;

import com.ratelimiter.model.data.TokenBucket;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Builder
public class TokenBucketRateLimiter implements RateLimiter {
    private final Map<String, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();
    private final Map<String, Long> blockedClients = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRefillTimes = new ConcurrentHashMap<>();

    /**
     * Checks if a client is allowed to make a request based on the rate limit settings.
     *
     * @param clientId the client identifier
     * @return true if the client is allowed to make a request, false otherwise
     */
    @Override
    public synchronized <T> boolean allow(String clientId, T bucket) {
        if (bucket instanceof TokenBucket) {
            TokenBucket tokenBucket = (TokenBucket) bucket;

            long currentTime = System.currentTimeMillis();

            if (isBlocked(clientId, currentTime)) return false;

            if (isBlockThresholdExceeded(clientId, tokenBucket)) {
                blockClient(clientId, currentTime, tokenBucket);
                return false;
            }

            if (isRateExceeded(clientId, currentTime, tokenBucket)) {
                incrementViolationCount(clientId);
                return false;
            }

            refillTokens(clientId, currentTime, tokenBucket);

            int tokenCount = tokens.getOrDefault(clientId, 0);

            if (tokenCount >= 1) {
                tokens.put(clientId, tokenCount - 1);
                return true;
            } else {
                lastRequestTimes.put(clientId, currentTime);
                return false;
            }
        }
        throw new IllegalArgumentException("Unsupported bucket type: " + bucket.getClass().getSimpleName());
    }

    protected boolean isBlockThresholdExceeded(String clientId, TokenBucket tokenBucket) {
        Integer violationCount = violationCounts.getOrDefault(clientId, 0);
        return violationCount >= tokenBucket.getBlockThreshold();
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

    protected void blockClient(String clientId, long currentTime, TokenBucket tokenBucket) {
        long blockedUntil = (long) (currentTime + tokenBucket.getBlockDurationMillis());
        blockedClients.put(clientId, blockedUntil);
    }

    private boolean isRateExceeded(String clientId, long currentTime, TokenBucket tokenBucket) {
        Long lastRequestTime = lastRequestTimes.get(clientId);
        if (lastRequestTime != null) {
            long timeElapsed = currentTime - lastRequestTime;
            double tokenCount = tokens.getOrDefault(clientId, 0);

            if (tokenCount < 1 && timeElapsed <= tokenBucket.getRefillTimeMillis()) {
                return true;
            }
        }
        lastRequestTimes.put(clientId, currentTime);
        return false;
    }

    protected void incrementViolationCount(String clientId) {
        violationCounts.compute(clientId, (key, value) -> value == null ? 1 : value + 1);
    }


    private void refillTokens(String clientId, long currentTime, TokenBucket tokenBucket) {
        tokens.compute(clientId, (key, value) -> {
            if(value == null) {
                return tokenBucket.getCapacity();
            }
            double currentTokens = tokens.getOrDefault(clientId, 0);
            long timeElapsed = currentTime - lastRefillTimes.getOrDefault(clientId, currentTime);

            double tokensToAdd = tokenBucket.getRefillRate() * (timeElapsed / tokenBucket.getRefillTimeMillis());
            double newTokens = currentTokens + tokensToAdd;

            int token = (int) Math.min(newTokens, tokenBucket.getCapacity());
            token = (int) Math.max(token, 0.0);
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
    public <T> long getRemainingTimeSec(String clientId, T bucket) {
        if (bucket instanceof TokenBucket) {
            TokenBucket tokenBucket = (TokenBucket) bucket;
            long currentTime = System.currentTimeMillis();
            Long lastRequestTime = lastRequestTimes.get(clientId);
            Long blockedTime = blockedClients.getOrDefault(clientId, lastRequestTime);

            if (lastRequestTime == null) {
                return 0;
            } else {
                long timeElapsed = currentTime - blockedTime;
                long remainingTime = (long) (tokenBucket.getRefillTimeMillis() - (timeElapsed % tokenBucket.getRefillTimeMillis()));
                return remainingTime / 1000;
            }
        }
        throw new IllegalArgumentException("Unsupported bucket type: " + bucket.getClass().getSimpleName());
    }

}
