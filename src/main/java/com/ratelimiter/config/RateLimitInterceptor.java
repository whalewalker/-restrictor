package com.ratelimiter.config;

import com.ratelimiter.service.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    @Value("${rateLimit.message}")
    private String message;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientId(request);
        if(!rateLimiter.allow(clientId)){
            String remainingTime = Long.toString(rateLimiter.getRemainingTimeMillis(clientId) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            String responseMessage = String.format("%s. Limit will reset in %s seconds", getMessage() , remainingTime);
            response.getWriter().write(responseMessage);
            return false;
        }
        return true;
    }

    private String getClientId(HttpServletRequest request) {
        // extract client id from request header, cookie or query param
        return null;
    }

    private String getMessage(){
        if (message == null)
            return "Rate limit message";
        return message;
    }

}
