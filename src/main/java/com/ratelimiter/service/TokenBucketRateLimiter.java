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

    @Override
    public synchronized boolean allow(String clientId) {
        double tokenCount = tokens.compute(clientId, (key, value) -> {
            if (value == null) {
                return capacity;
            } else {
                double newTokens = value + refillRate * (System.currentTimeMillis() - System.nanoTime()) / 1e9;
                return Math.min(newTokens, capacity);
            }
        });
        if (tokenCount >= 1) {
            tokens.put(clientId, tokenCount - 1);
            return true;
        } else {
            return false;
        }
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
