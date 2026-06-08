package com.erumpay.auth_service.common.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PinHashUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[32];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hashPin(String pin, String salt) {
        try {
            String salted = pin + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("PIN 해시 실패", e);
        }
    }

    public boolean verifyPin(String pin, String salt, String storedHash) {
        return hashPin(pin, salt).equals(storedHash);
    }

    public boolean isWeakPattern(String pin) {
        // 동일 숫자 4자리 이상
        for (int i = 0; i <= pin.length() - 4; i++) {
            if (pin.charAt(i) == pin.charAt(i + 1)
                && pin.charAt(i) == pin.charAt(i + 2)
                && pin.charAt(i) == pin.charAt(i + 3)) {
                return true;
            }
        }
        // 연속 증가 4자리 이상
        for (int i = 0; i <= pin.length() - 4; i++) {
            if (pin.charAt(i + 1) == pin.charAt(i) + 1
                && pin.charAt(i + 2) == pin.charAt(i) + 2
                && pin.charAt(i + 3) == pin.charAt(i) + 3) {
                return true;
            }
        }
        // 연속 감소 4자리 이상
        for (int i = 0; i <= pin.length() - 4; i++) {
            if (pin.charAt(i + 1) == pin.charAt(i) - 1
                && pin.charAt(i + 2) == pin.charAt(i) - 2
                && pin.charAt(i + 3) == pin.charAt(i) - 3) {
                return true;
            }
        }
        return false;
    }
}
