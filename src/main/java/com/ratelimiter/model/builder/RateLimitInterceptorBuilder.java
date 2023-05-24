package com.ratelimiter.model.builder;

import com.ratelimiter.config.RateLimitInterceptor;
import com.ratelimiter.model.data.RateLimitType;
import com.ratelimiter.service.RateLimiter;

import static com.ratelimiter.service.RateLimiterFactory.createRateLimiter;

public class RateLimitInterceptorBuilder<T> {
    private RateLimitType rateLimitType;
    private String message;
    private String secretKey;
    private T limitConfig;

    public RateLimitInterceptorBuilder<T> rateLimitType(RateLimitType rateLimitType) {
        this.rateLimitType = rateLimitType;
        return this;
    }

    public RateLimitInterceptorBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    public RateLimitInterceptorBuilder<T> secretKey(String secretKey) {
        this.secretKey = secretKey;
        return this;
    }

    public RateLimitInterceptorBuilder<T> limitConfig(Object config) {
        this.limitConfig = (T) config;
        return this;
    }

    public RateLimitInterceptor<T> build() {
        RateLimiter defaultRatelimiter = createRateLimiter(RateLimitType.TOKEN_BUCKET);

        if (rateLimitType != null) {
            RateLimiter rateLimiter = createRateLimiter(rateLimitType);
            return new RateLimitInterceptor<>(rateLimiter, message, secretKey, limitConfig);
        }
        return new RateLimitInterceptor<>(defaultRatelimiter, message, secretKey, limitConfig);
    }

}