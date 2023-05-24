package com.ratelimiter.model.data;

import com.ratelimiter.annotations.Restrict;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenBucket {
    private final Integer capacity;
    private final Integer refillRate;
    private final Double refillTimeMillis;
    private final Double blockDurationMillis;
    private final Integer blockThreshold;

    public static TokenBucket createBucket(Restrict annotation) {
        int capacity = annotation.capacity();
        int refillRate = annotation.refillRate();
        double refillTimeMillis = annotation.refillTimeMillis();
        double blockDurationMillis = annotation.blockDurationMillis();
        int blockThreshold = annotation.blockThreshold();
        return TokenBucket.builder()
                .capacity(capacity)
                .refillTimeMillis(refillTimeMillis)
                .refillRate(refillRate)
                .blockThreshold(blockThreshold)
                .blockDurationMillis(blockDurationMillis)
                .build();
    }
}

