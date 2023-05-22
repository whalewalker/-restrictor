package com.ratelimiter.config;

import com.ratelimiter.annotations.Restrict;
import com.ratelimiter.model.data.Bucket;
import com.ratelimiter.model.data.RateLimitType;
import com.ratelimiter.service.RateLimiter;
import com.ratelimiter.service.RateLimiterFactory;
import com.ratelimiter.service.RequestSignatureUtil;
import com.ratelimiter.service.TokenBucketRateLimiter;
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
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterFactory rateLimiterFactory;

    @Value("${rateLimit.message}")
    private String message;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Restrict annotation = getRestrictAnnotation(handlerMethod.getMethod());

            if (annotation != null) {
                RateLimiter rateLimiter = rateLimiterFactory.createRateLimiter(RateLimitType.TOKEN_BUCKET, annotation);
                if (!validateRequestSignature(request, response, handler, annotation)) {
                    return false;
                }

                String clientId = getClientId(request);
                if(!rateLimiter.allow(clientId)){
                    String remainingTime = Long.toString(rateLimiter.getRemainingTimeSec(clientId));
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    String responseMessage = String.format("%s. Limit will reset in %s seconds", getMessage() , remainingTime);
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
        String clientId = annotation.clientId();
        if (clientId.isEmpty()) {
            clientId = getIpAddress(request);
        }
        return clientId;
    }

    private String getClientId(HttpServletRequest request){
        return request.getHeader("X-Request-Signature");
    }


    private String getMessage(){
        if (message == null)
            return "Rate limit message";
        return message;
    }


    private String getRequestString(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private String getClassName(Object handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            return handlerMethod.getBeanType().getSimpleName();
        }
        return "";
    }

    private boolean validateRequestSignature(HttpServletRequest request, HttpServletResponse response, Object handler, Restrict annotation) throws NoSuchAlgorithmException, InvalidKeyException {
        String requestString = getRequestString(request);
        String clientId = getClientId(request, annotation);
        String className = getClassName(handler);

        String receivedSignature = request.getHeader("X-Request-Signature");

        // Check if requestString is present, otherwise use className
        String signatureInput = Optional.of(requestString).orElse(className);

        String newSignature = RequestSignatureUtil.generateRequestSignature(signatureInput, clientId);

        if (receivedSignature == null || !receivedSignature.equals(newSignature)) {
            response.setHeader("X-Request-Signature", newSignature);
        }

        boolean isSignatureValid = RequestSignatureUtil.verifyRequestSignature(signatureInput, receivedSignature, clientId);

        if (!isSignatureValid) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        return true;
    }



}
