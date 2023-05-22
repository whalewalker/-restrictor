package com.ratelimiter.model.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Bucket {
    private final Double capacity;
    private final Double refillRate;
    private final Double refillTimeMillis;
    private final Long blockDurationMillis;
    private final Integer blockThreshold;
}
