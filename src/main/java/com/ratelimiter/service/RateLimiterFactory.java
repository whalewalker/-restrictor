package com.ratelimiter.service;

import com.ratelimiter.annotations.Restrict;
import com.ratelimiter.model.data.Bucket;
import com.ratelimiter.model.data.RateLimitType;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterFactory {
    public RateLimiter createTokenBucketRateLimiter(Restrict annotation) {
        double capacity = annotation.capacity();
        double refillRate = annotation.refillRate();
        double refillTimeMillis = annotation.refillTimeMillis();
        long blockDurationMillis = annotation.blockDurationMillis();
        int blockThreshold = annotation.blockThreshold();

        Bucket bucket = Bucket.builder()
                .capacity(capacity)
                .refillRate(refillRate)
                .refillTimeMillis(refillTimeMillis)
                .blockDurationMillis(blockDurationMillis)
                .blockThreshold(blockThreshold)
                .build();

        return new TokenBucketRateLimiter(capacity, refillRate, refillTimeMillis, blockDurationMillis, blockThreshold);
    }

    public RateLimiter createRateLimiter(RateLimitType rateLimitType, Restrict annotation) {
        switch (rateLimitType) {
            case TOKEN_BUCKET:
                return createTokenBucketRateLimiter(annotation);
            case OTHER_TYPE:
            // Add more cases for other rate limiting types
            default:
                throw new IllegalArgumentException("Unsupported rate limiting type: " + rateLimitType);
        }
    }
}
