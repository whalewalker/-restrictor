package com.ratelimiter.service;

import org.springframework.stereotype.Service;

@Service
public class TokenBucketRateLimiter implements RateLimiter{
    @Override
    public boolean allow(String clientId) {
        return  false;
    }

    @Override
    public long getRemainingTimeMillis(String clientId) {
        return 0;
    }
}
