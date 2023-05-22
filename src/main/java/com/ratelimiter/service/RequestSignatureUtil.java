package com.ratelimiter.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public  class RequestSignatureUtil {
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
}
