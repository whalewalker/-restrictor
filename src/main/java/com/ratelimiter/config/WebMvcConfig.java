package com.ratelimiter.config;

import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.RateLimiterFactory;
import com.ratelimiter.service.RequestSignatureUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final RateLimiterFactory rateLimiterFactory;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(rateLimiterFactory));
    }
}
