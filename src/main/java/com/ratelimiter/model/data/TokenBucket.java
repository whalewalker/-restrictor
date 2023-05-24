package com.ratelimiter.model.data;

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
}
