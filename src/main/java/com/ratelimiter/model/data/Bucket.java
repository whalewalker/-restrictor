package com.ratelimiter.model.data;

import lombok.*;

@Data
@Builder
public class Bucket {
    private final Double capacity;
    private final Double refillRate;
    private final Double refillTimeMillis;
    private Long blockDurationMillis;
    private Integer blockThreshold;
}
