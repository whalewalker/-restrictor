package com.ratelimiter.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Restrict {
    double permitsPerSecond() default 10.0; // Specifies the rate limit in permits per second.
    long warmupPeriod() default 0L; // Specifies the duration of the warmup period in milliseconds (default is 0, indicating no warmup period).
}
