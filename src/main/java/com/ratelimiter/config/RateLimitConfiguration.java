package com.ratelimiter.config;

import com.ratelimiter.model.data.RateLimitType;
import com.ratelimiter.service.RateLimiterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class RateLimitConfiguration {
    private final RateLimiterFactory rateLimiterFactory;
    @Bean
    public RateLimitInterceptor rateLimitInterceptor() {
        return  RateLimitInterceptor.builder()
                .rateLimiter(rateLimiterFactory.createRateLimiter(RateLimitType.TOKEN_BUCKET))
                .message("Custom message")
                .secretKey("new secret key")
                .build();
    }

}
