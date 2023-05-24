package com.ratelimiter.service;

import com.ratelimiter.annotations.Restrict;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public  class RequestSignatureUtil {
    private static final String UNKNOWN_MESSAGE = "unknown";
    private static final String REQUEST_SIGNATURE = "X-Request-Signature";
    private static final String ALGORITHM = "HmacSHA256";
    private static final String SHARED_SECRET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateRequestSignature(String requestString, String clientId, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String combinedString = requestString + clientId;
        return generateSignature(combinedString, secretKey);
    }

    private static String generateSignature(String data, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmacSha256 = Mac.getInstance(ALGORITHM);
        String key = (secretKey != null &&  !secretKey.isEmpty()) ? secretKey : SHARED_SECRET;
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        hmacSha256.init(secretKeySpec);

        byte[] signatureBytes = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public static boolean verifyRequestSignature(String requestString, String receivedSignature, String clientId, String secretKey) {
        try {
            String expectedSignature = generateRequestSignature(requestString, clientId, secretKey);
            return expectedSignature.equals(receivedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return false;
        }
    }


    private static String getIpAddress(HttpServletRequest request) {
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

    public   static void validateRequestSignature(HttpServletRequest request, HttpServletResponse response, Restrict annotation, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String clientId = getClientId(request, annotation);
        processRequestSignature(request, response, clientId, secretKey);
    }

    public static void validateRequestSignature(HttpServletRequest request, HttpServletResponse response, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
        String clientId = getClientId(request, null);
        processRequestSignature(request, response, clientId, secretKey);
    }


    private static String getClientId(HttpServletRequest request, Restrict annotation) {
        if (annotation != null && !annotation.userId().isEmpty()) {
            return annotation.userId();
        }
        return getIpAddress(request);
    }

    private  static void processRequestSignature(HttpServletRequest request, HttpServletResponse response, String clientId, String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
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

    private static String getRequestString(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    private static String getClassName(HandlerMethod handler) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = handler;
            return handlerMethod.getBeanType().getSimpleName();
        }
        return "";
    }


    public static String getClientId(HttpServletResponse response) {
        return response.getHeader(REQUEST_SIGNATURE);
    }

}
