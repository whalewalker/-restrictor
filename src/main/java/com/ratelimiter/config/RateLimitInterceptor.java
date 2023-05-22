package com.ratelimiter.config;

import com.ratelimiter.annotations.Restrict;
import com.ratelimiter.model.data.Bucket;
import com.ratelimiter.model.data.RateLimitType;
import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.RateLimiterFactory;
import com.ratelimiter.service.RequestSignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimiter rateLimiter;
    private static final String UNKNOWN_MESSAGE = "unknown";
    private static final String REQUEST_SIGNATURE = "X-Request-Signature";


    public RateLimitInterceptor(RateLimiterFactory rateLimiterFactory) {
        this.rateLimiter = rateLimiterFactory.createRateLimiter(RateLimitType.TOKEN_BUCKET);
    }

    @Value("${rateLimit.message}")
    private String message;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Restrict annotation = getRestrictAnnotation(handlerMethod.getMethod());

            if (annotation != null) {
                Bucket bucket = createBucket(annotation);
                validateRequestSignature(request, response,  annotation);

                String clientId = getClientId(response);
                if (!rateLimiter.allow(clientId, bucket)) {
                    String remainingTime = Long.toString(rateLimiter.getRemainingTimeSec(clientId, bucket));
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    String responseMessage = String.format("%s. Limit will reset in %s seconds", getMessage(), remainingTime);
                    response.getWriter().write(responseMessage);
                    return false;
                }
            }
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
        String clientId = annotation.userId();
        if (clientId.isEmpty()) {
            clientId = getIpAddress(request);
        }
        return clientId;
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

    private void validateRequestSignature(HttpServletRequest request, HttpServletResponse response, Restrict annotation) throws NoSuchAlgorithmException, InvalidKeyException {
        String signatureInput  = getRequestString(request);
        String clientId = getClientId(request, annotation);
        String receivedSignature = request.getHeader(REQUEST_SIGNATURE);
        String newSignature = RequestSignatureUtil.generateRequestSignature(signatureInput, clientId);

        if (receivedSignature != null && receivedSignature.equals(newSignature)) {
            boolean isSignatureValid = RequestSignatureUtil.verifyRequestSignature(signatureInput, receivedSignature, clientId);
            if (isSignatureValid) {
                return;
            }
        }
        response.setHeader(REQUEST_SIGNATURE, newSignature);
    }


    private Bucket createBucket(Restrict annotation) {
        double capacity = annotation.capacity();
        double refillRate = annotation.refillRate();
        double refillTimeMillis = annotation.refillTimeMillis();
        long blockDurationMillis = annotation.blockDurationMillis();
        int blockThreshold = annotation.blockThreshold();
        return Bucket.builder()
                .capacity(capacity)
                .refillTimeMillis(refillTimeMillis)
                .refillRate(refillRate)
                .blockThreshold(blockThreshold)
                .blockDurationMillis(blockDurationMillis)
                .build();

    }

}
