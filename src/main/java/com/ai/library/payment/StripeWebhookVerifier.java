package com.ai.library.payment;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Minimal verifier for Stripe webhook signatures (t=timestamp,v1=signature)
 * This is intentionally small: it computes HMAC-SHA256 over the payload prefixed by timestamp.
 * Note: in production prefer using Stripe official SDK verification helpers.
 */
@Component
public class StripeWebhookVerifier {
    private static final Logger log = LoggerFactory.getLogger(StripeWebhookVerifier.class);

    /**
     * Verify a Stripe webhook signature.
     *
     * - Accepts headers in the form: t=timestamp,v1=signature[,v1=...]
     * - Verifies at least one v1 signature matches the HMAC-SHA256 of "{t}.{payload}" using the provided secret
     * - Enforces a timestamp tolerance (seconds) to mitigate replay attacks
     *
     * @param payload the raw request body
     * @param signatureHeader the value of the Stripe-Signature header
     * @param secret the webhook signing secret
     * @param toleranceSeconds allowed clock skew / replay window in seconds (recommended 300)
     * @return true if signature and timestamp checks pass
     */
    public boolean verify(String payload, String signatureHeader, String secret, long toleranceSeconds) {
        if (signatureHeader == null || signatureHeader.isBlank() || secret == null || secret.isBlank()) return false;
        try {
            // header looks like: t=timestamp,v1=signature[,v1=...]
            String[] parts = signatureHeader.split(",");
            String t = null;
            List<String> v1s = new ArrayList<>();
            for (String p : parts) {
                String[] kv = p.split("=", 2);
                if (kv.length != 2) continue;
                if (kv[0].equals("t")) t = kv[1];
                if (kv[0].equals("v1")) v1s.add(kv[1]);
            }
            if (t == null || v1s.isEmpty()) return false;

            long timestamp;
            try {
                timestamp = Long.parseLong(t);
            } catch (NumberFormatException nfe) {
                log.warn("Invalid stripe webhook timestamp: {}", t);
                return false;
            }

            long now = Instant.now().getEpochSecond();
            long age = Math.abs(now - timestamp);
            if (age > toleranceSeconds) {
                log.warn("Stripe webhook timestamp outside tolerance (age={}s, allowed={}s)", age, toleranceSeconds);
                return false;
            }

            String signedPayload = t + "." + payload;
            byte[] mac = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmac(signedPayload);
            String expected = bytesToHex(mac);

            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            for (String candidate : v1s) {
                if (constantTimeEquals(expectedBytes, candidate.getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
            }

            return false;
        } catch (Exception ex) {
            log.error("Error verifying stripe webhook", ex);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        // Use MessageDigest.isEqual which is constant-time for byte array comparison
        try {
            return MessageDigest.isEqual(a, b);
        } catch (Exception ex) {
            // fallback to manual compare
            if (a == null || b == null) return false;
            if (a.length != b.length) return false;
            int result = 0;
            for (int i = 0; i < a.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
