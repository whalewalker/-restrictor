package com.ratelimiter.config;

import com.ratelimiter.model.data.RateLimitType;
import com.ratelimiter.model.data.TokenBucket;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class RateLimitConfiguration {
    @Bean
    public RateLimitInterceptor<?> rateLimitInterceptor() {

        TokenBucket tokenBucket = TokenBucket.builder()
                .capacity(2)
                .refillRate(1)
                .refillTimeMillis(6000.0)
                .blockThreshold(10)
                .blockDurationMillis(6000.0)
                .build();


        return  RateLimitInterceptor.builder()
                .rateLimitType(RateLimitType.TOKEN_BUCKET)
                .rateLimitData(tokenBucket)
                .message("Custom message")
                .secretKey("created a new secret key")
                .build();
    }

}
