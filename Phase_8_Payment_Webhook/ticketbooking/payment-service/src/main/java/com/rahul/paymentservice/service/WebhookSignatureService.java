package com.rahul.paymentservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

// HMAC-SHA256 over the fields that matter. Only someone who knows the shared secret can produce it.
@Service
public class WebhookSignatureService {

    public static final String SIGNATURE_HEADER = "X-Signature";
    private static final String ALGORITHM = "HmacSHA256";

    private final String secret;

    public WebhookSignatureService(@Value("${payment.webhook.secret}") String secret) {
        this.secret = secret;
    }

    public String sign(String eventId, Long paymentId, String status) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            String text = eventId + "|" + paymentId + "|" + status;
            return HexFormat.of().formatHex(mac.doFinal(text.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not sign the webhook", e);
        }
    }

    public boolean isValid(String eventId, Long paymentId, String status, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] expected = sign(eventId, paymentId, status).getBytes(StandardCharsets.UTF_8);
        // constant-time compare: a normal equals() leaks, through timing, how many characters matched
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}