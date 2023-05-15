package com.ratelimiter.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TokenBucketRateLimiter implements RateLimiter{
    private final double capacity;
    private final double refillRate;
    private final Map<String, Double> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTimes = new ConcurrentHashMap<>();



    @Override
    public synchronized boolean allow(String clientId) {
        long currentTime = System.currentTimeMillis();

        // Check if the client has exceeded the rate limit
        if (isRateExceeded(clientId, currentTime)) {
            // Block the client
            return false;
        }

        // Update token count
        double tokenCount = updateTokenCount(clientId, currentTime);

        // Check if there are enough tokens for the request
        if (tokenCount >= 1) {
            tokens.put(clientId, tokenCount - 1);
            return true;
        } else {
            // Block the client
            return false;
        }
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
        if (tokenCount >= 1)
            return 0;
        else {
            double remainingTimeSeconds = tokenCount / refillRate;
            return (long) (remainingTimeSeconds * 1000);
        }
    }

}
