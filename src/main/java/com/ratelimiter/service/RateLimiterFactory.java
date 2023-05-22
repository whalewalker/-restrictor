package com.ratelimiter.service;

import com.ratelimiter.model.data.RateLimitType;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
@Builder
public class RateLimiterFactory {
    public TokenBucketRateLimiter createTokenBucketRateLimiter() {
        return new TokenBucketRateLimiter();
    }

    public RateLimiter createRateLimiter(RateLimitType rateLimitType) {
        switch (rateLimitType) {
            case TOKEN_BUCKET:
                return createTokenBucketRateLimiter();
            case OTHER_TYPE:
            // Add more cases for other rate limiting types
            default:
                throw new IllegalArgumentException("Unsupported rate limiting type: " + rateLimitType);
        }
    }
}
