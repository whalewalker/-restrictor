package com.ratelimiter.service;

public interface RateLimiter {
    boolean allow(String clientId);
    long getRemainingTimeMillis(String clientId);
}
