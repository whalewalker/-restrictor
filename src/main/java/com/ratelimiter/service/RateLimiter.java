package com.ratelimiter.service;

public interface RateLimiter {
    <T> boolean allow(String clientId, T bucket);
    <T> long getRemainingTimeSec(String clientId, T bucket);
}
