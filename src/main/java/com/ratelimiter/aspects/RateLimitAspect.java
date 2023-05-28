package com.ratelimiter.aspects;

import com.google.common.util.concurrent.RateLimiter;
import com.ratelimiter.annotations.Restrict;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {
    @Around("@annotation(restrict)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint, Restrict restrict) throws Throwable {
        RateLimiter rateLimiter = createRateLimiter(restrict);

        rateLimiter.acquire(); // Apply rate limiting before the method call
        return joinPoint.proceed(); // Proceed with the method execution
    }

    private RateLimiter createRateLimiter(Restrict restrict) {
        if (restrict.warmupPeriod() > 0L)
            return RateLimiter.create(restrict.permitsPerSecond(), restrict.warmupPeriod(), TimeUnit.MILLISECONDS);
        return RateLimiter.create(restrict.permitsPerSecond());
    }
}