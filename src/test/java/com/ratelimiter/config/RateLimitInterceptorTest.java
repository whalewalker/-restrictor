package com.ratelimiter.config;

import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitInterceptorTest {
    private RateLimiter rateLimiter;
    private RateLimitInterceptor rateLimitInterceptor;

    @BeforeEach
    void setUp() {
        rateLimiter = new TokenBucketRateLimiter();
        rateLimitInterceptor = new RateLimitInterceptor(rateLimiter);
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = rateLimitInterceptor.preHandle(request, response, null);

        assertFalse(result);
        assertEquals(429, response.getStatus());
        assertEquals("Rate limit is exceeded", response.getContentAsString());
    }


}