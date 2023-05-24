package com.ratelimiter.config;

import com.ratelimiter.annotations.Restrict;
import com.ratelimiter.model.builder.RateLimitInterceptorBuilder;
import com.ratelimiter.model.data.TokenBucket;
import com.ratelimiter.service.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;

import static com.ratelimiter.service.RequestSignatureUtil.getClientId;
import static com.ratelimiter.service.RequestSignatureUtil.validateRequestSignature;

@Slf4j
public class RateLimitInterceptor<T> implements HandlerInterceptor {

    private final RateLimiter rateLimiter;
    private final String message;
    private final T limitConfig;
    private final String secretKey;

    public RateLimitInterceptor(RateLimiter rateLimiter, String message, String secretKey, T data) {
        this.rateLimiter = rateLimiter;
        this.message = message;
        this.secretKey = secretKey;
        this.limitConfig = data;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Restrict annotation = getRestrictAnnotation(handlerMethod.getMethod());

            if (annotation != null) {
                TokenBucket tokenBucket = TokenBucket.createBucket(annotation);
                validateRequestSignature(request, response, annotation, secretKey);
                return processRateLimiting(response, tokenBucket);
            } else if (limitConfig != null) {
                validateRequestSignature(request, response, secretKey);
                TokenBucket bucket = (TokenBucket) limitConfig;
                return processRateLimiting(response, bucket);
            }
        }
        return true;
    }

    private boolean processRateLimiting(HttpServletResponse response, TokenBucket tokenBucket) {
        String clientId = getClientId(response);
        if (!rateLimiter.allow(clientId, tokenBucket)) {
            String remainingTime = Long.toString(rateLimiter.getRemainingTimeSec(clientId, tokenBucket));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            String responseMessage = String.format("%s. Limit will reset in %s seconds", getMessage(), remainingTime);

            try {
                response.getWriter().write(responseMessage);
            } catch (IOException e) {
                response.reset();
                return false;
            }
            return false;
        }
        return true;
    }

    private Restrict getRestrictAnnotation(Method method) {
        Restrict annotation = method.getAnnotation(Restrict.class);
        if (annotation == null) {
            // Check if the annotation is present at the class level
            Class<?> declaringClass = method.getDeclaringClass();
            annotation = declaringClass.getAnnotation(Restrict.class);
        }
        return annotation;
    }


    private String getMessage() {
        if (message == null)
            return "Rate limit message";
        return message;
    }


    public static RateLimitInterceptorBuilder<?> builder() {
        return new RateLimitInterceptorBuilder<>();
    }

}
