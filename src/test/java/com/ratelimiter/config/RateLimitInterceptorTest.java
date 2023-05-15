package com.ratelimiter.config;

import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource("classpath:application.properties")
class RateLimitInterceptorTest {
    private RateLimiter rateLimiter;
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        rateLimiter = getRateLimiter(1, 1);
        rateLimitInterceptor = new RateLimitInterceptor(rateLimiter);
    }

    private static TokenBucketRateLimiter getRateLimiter(int capacity, int refillRate) {
        return new TokenBucketRateLimiter(capacity, refillRate);
    }

    @Test
    void testPreHandle_whenAllow_thenProceed() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        assertTrue(result);
        assertEquals(200, response.getStatus());
    }

    @Test
    void testPreHandle_whenExceedLimit_thenReject() throws Exception {
        rateLimiter = getRateLimiter(1, 1);
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        rateLimitInterceptor.preHandle(request1, response1, null);

        boolean result = rateLimitInterceptor.preHandle(request2, response2, null);

        assertFalse(result);
        assertEquals(429, response2.getStatus());
        assertThat(response2.getContentAsString()).contains("Rate limit message");
    }


    @Test
    void testPreHandle_whenExceedLimit_thenRejectWithCustomMessage() throws Exception {
        ReflectionTestUtils.setField(rateLimitInterceptor, "message", "Custom rate limit message");
        rateLimiter = getRateLimiter(1, 1);
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        rateLimitInterceptor.preHandle(request1, response1, null);

        boolean result = rateLimitInterceptor.preHandle(request2, response2, null);

        assertFalse(result);
        assertEquals(429, response2.getStatus());
        assertThat(response2.getContentAsString()).contains("Custom rate limit message");
    }


}