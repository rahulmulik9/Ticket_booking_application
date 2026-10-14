package com.rahul.bookingservice.util;

import com.rahul.bookingservice.exception.InvalidIdempotencyKeyException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IdempotencyUtil {

    private static final int MAX_KEY_LENGTH = 64;

    private IdempotencyUtil() {
    }

    public static void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > MAX_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException("Idempotency-Key must be 1 to " + MAX_KEY_LENGTH + " characters");
        }
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}