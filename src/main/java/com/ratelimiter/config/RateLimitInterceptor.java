package com.ratelimiter.config;

import com.ratelimiter.annotations.Restrict;
import com.ratelimiter.model.data.RateLimitType;
import com.ratelimiter.model.data.TokenBucket;
import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.RequestSignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.ratelimiter.service.RateLimiterFactory.createRateLimiter;

@Slf4j
public class RateLimitInterceptor<T> implements HandlerInterceptor {
    private static final String UNKNOWN_MESSAGE = "unknown";
    private static final String REQUEST_SIGNATURE = "X-Request-Signature";
    private final RateLimiter rateLimiter;
    private final String message;
    private final T rateLimitData;
    private final String secretKey;

    public RateLimitInterceptor(RateLimiter rateLimiter, String message, String secretKey, T data) {
        this.rateLimiter = rateLimiter;
        this.message = message;
        this.secretKey = secretKey;
        this.rateLimitData = data;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Restrict annotation = getRestrictAnnotation(handlerMethod.getMethod());

            if (annotation != null) {
                TokenBucket tokenBucket = createBucket(annotation);
                validateRequestSignature(request, response, annotation, secretKey);
                return processRateLimiting(response, tokenBucket);
            } else if (rateLimitData != null) {
                validateRequestSignature(request, response, secretKey);
                TokenBucket bucket = (TokenBucket) rateLimitData;
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

    private String getClientId(HttpServletRequest request, Restrict annotation) {
        if (annotation != null && !annotation.userId().isEmpty()) {
            return annotation.userId();
        }
        return getIpAddress(request);
    }


    private String getClientId(HttpServletResponse response) {
        return response.getHeader(REQUEST_SIGNATURE);
    }


    private String getMessage() {
        if (message == null)
            return "Rate limit message";
        return message;
    }


    private String getRequestString(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || UNKNOWN_MESSAGE.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || UNKNOWN_MESSAGE.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || UNKNOWN_MESSAGE.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || UNKNOWN_MESSAGE.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || UNKNOWN_MESSAGE.equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private void validateRequestSignature(HttpServletRequest request, HttpServletResponse response, Restrict annotation, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String clientId = getClientId(request, annotation);
        processRequestSignature(request, response, clientId, secretKey);
    }

    private void validateRequestSignature(HttpServletRequest request, HttpServletResponse response, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String clientId = getClientId(request, null);
        processRequestSignature(request, response, clientId, secretKey);
    }

    private void processRequestSignature(HttpServletRequest request, HttpServletResponse response, String clientId, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String receivedSignature = request.getHeader(REQUEST_SIGNATURE);
        String signatureInput = getRequestString(request);

        String newSignature = RequestSignatureUtil.generateRequestSignature(signatureInput, clientId, secretKey);

        if (receivedSignature != null && receivedSignature.equals(newSignature)) {
            boolean isSignatureValid = RequestSignatureUtil.verifyRequestSignature(signatureInput, receivedSignature, clientId, secretKey);
            if (isSignatureValid) {
                return;
            }
        }

        response.setHeader(REQUEST_SIGNATURE, newSignature);
    }


    private String getClassName(HandlerMethod handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = handler;
            return handlerMethod.getBeanType().getSimpleName();
        }
        return "";
    }

    private TokenBucket createBucket(Restrict annotation) {
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


    public static RateLimitInterceptorBuilder<?> builder() {
        return new RateLimitInterceptorBuilder<>();
    }

    public static class RateLimitInterceptorBuilder<T> {
        private RateLimitType rateLimitType;
        private String message;
        private String secretKey;
        private T rateLimitData;


        public RateLimitInterceptorBuilder<T> rateLimitType(RateLimitType rateLimitType) {
            this.rateLimitType = rateLimitType;
            return this;
        }

        public RateLimitInterceptorBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public RateLimitInterceptorBuilder<T> secretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public RateLimitInterceptorBuilder<T> rateLimitData(Object rateLimiterData) {
            this.rateLimitData = (T) rateLimiterData;
            return this;
        }

        public RateLimitInterceptor<T> build() {
            RateLimiter defaultRatelimiter = createRateLimiter(RateLimitType.TOKEN_BUCKET);

            if (rateLimitType != null) {
                RateLimiter rateLimiter = createRateLimiter(rateLimitType);
                return new RateLimitInterceptor<>(rateLimiter, message, secretKey, rateLimitData);
            }
            return new RateLimitInterceptor<>(defaultRatelimiter, message, secretKey, rateLimitData);
        }

    }
}
