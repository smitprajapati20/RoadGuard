package com.app.roadguard;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted PBKDF2 hashing for passwords and security answers. Never store plain text. */
public class PasswordUtil {

    private static final int ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;

    public static String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hash(String value, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(value.toCharArray(),
                    Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return Base64.encodeToString(factory.generateSecret(spec).getEncoded(), Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }

    public static boolean verify(String value, String salt, String expectedHash) {
        if (salt == null || salt.isEmpty() || expectedHash == null || expectedHash.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(hash(value, salt).getBytes(), expectedHash.getBytes());
    }

    /** Security answers are compared case-insensitively and without extra spaces. */
    public static String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase(Locale.ROOT);
    }
}
