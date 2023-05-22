package com.ratelimiter.service;

import com.ratelimiter.model.data.Bucket;

public interface RateLimiter {
    boolean allow(String clientId, Bucket bucket);
    long getRemainingTimeSec(String clientId, Bucket bucket);
}
