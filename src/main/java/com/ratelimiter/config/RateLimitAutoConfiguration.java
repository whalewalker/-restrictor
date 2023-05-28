package com.ratelimiter.config;


import com.ratelimiter.aspects.RateLimitAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
@Configuration
@ConditionalOnClass(RateLimitAspect.class)
public class RateLimitAutoConfiguration {
    // Configure and initialize rate limiting components, if required
}
