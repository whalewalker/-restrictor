package com.ratelimiter.config;

import com.ratelimiter.service.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiter rateLimiter;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientId(request);
        if(!rateLimiter.allow(clientId)){
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit is exceeded");
            return false;
        }
        return true;
    }

    private String getClientId(HttpServletRequest request) {
        // extract client id from request header, cookie or query param
        return null;
    }

}
