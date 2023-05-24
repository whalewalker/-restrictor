package com.ratelimiter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Restrict {
    int capacity() default 1000;
    int refillRate() default 100;
    double refillTimeMillis() default 60000;
    int blockDurationMillis() default 60000;
    int blockThreshold() default 10;
    String userId() default "";
}
