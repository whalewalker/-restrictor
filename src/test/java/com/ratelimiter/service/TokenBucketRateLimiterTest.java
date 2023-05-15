package com.ratelimiter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
     void testAllowMethod() {
        // Test case 1: Allow requests if enough tokens are available
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(2, 1);
        boolean allowRequest = rateLimiter.allow("client1");
        assertTrue(allowRequest);

        // Test case 2: Block requests if tokens are exhausted
        rateLimiter = new TokenBucketRateLimiter(2, 1);
        rateLimiter.allow("client1");
        rateLimiter.allow("client1");
        allowRequest = rateLimiter.allow("client1");
        assertFalse(allowRequest);
    }

    @Test
     void testTokenRefill() throws InterruptedException {
        // Test case 1: Refill tokens after a delay
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(2, 1);
        rateLimiter.allow("client1");
        rateLimiter.allow("client1");
        boolean allowRequest = rateLimiter.allow("client1");
        assertFalse(allowRequest);
        Thread.sleep(1000); // Wait for token refill
        allowRequest = rateLimiter.allow("client1");
        assertTrue(allowRequest);

        // Test case 2: Refill tokens after a delay and cap at max capacity
        rateLimiter = new TokenBucketRateLimiter(2, 0.5);
        rateLimiter.allow("client1");
        Thread.sleep(2000); // Wait for token refill
        boolean allowRequest1 = rateLimiter.allow("client1");
        boolean allowRequest2 = rateLimiter.allow("client1");
        boolean allowRequest3 = rateLimiter.allow("client1");
        assertTrue(allowRequest1);
        assertTrue(allowRequest2);
        assertFalse(allowRequest3);
    }


   @Test
    void testGetRemainingTimeMillis_EnoughTokens_ReturnsZero() {
      // Arrange
      TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(10, 1);

      // Act
      long remainingTimeMillis = rateLimiter.getRemainingTimeMillis("client1");

      // Assert
      assertEquals(0, remainingTimeMillis, "Remaining time should be zero");
   }

   @Test
    void testGetRemainingTimeMillis_TokensExhausted_ReturnsCorrectTime() {
      // Arrange
      TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(2, 1);
      rateLimiter.allow("client1");
      rateLimiter.allow("client1");

      // Act
      long remainingTimeMillis = rateLimiter.getRemainingTimeMillis("client1");

      // Assert
      assertTrue(remainingTimeMillis > 0, "Remaining time should be greater than zero");
      assertTrue(remainingTimeMillis <= 1000, "Remaining time should be less than or equal to 1000 milliseconds");
   }

   @Test
    void testGetRemainingTimeMillis_ClientNeverRequested_ReturnsMaxTime() {
      // Arrange
      TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(10, 1);

      // Act
      long remainingTimeMillis = rateLimiter.getRemainingTimeMillis("client1");

      // Assert
      assertEquals(Long.MAX_VALUE, remainingTimeMillis, "Remaining time should be Long.MAX_VALUE");
   }



}