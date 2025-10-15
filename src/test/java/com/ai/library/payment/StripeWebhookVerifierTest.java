package com.ai.library.payment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

public class StripeWebhookVerifierTest {

    @Test
    void verify_validSignature_returnsTrue() {
        StripeWebhookVerifier v = new StripeWebhookVerifier();
        String payload = "{\"hello\":\"world\"}";
        long ts = Instant.now().getEpochSecond();
        String secret = "whsec_test_secret";
        String signed = ts + "." + payload;
        // compute expected using HmacUtils similarly to implementation
        byte[] mac = new org.apache.commons.codec.digest.HmacUtils(org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256, secret).hmac(signed);
        String expected = bytesToHex(mac);
        String header = "t=" + ts + ",v1=" + expected;

        Assertions.assertTrue(v.verify(payload, header, secret, 300L));
    }

    @Test
    void verify_oldTimestamp_returnsFalse() {
        StripeWebhookVerifier v = new StripeWebhookVerifier();
        String payload = "{}";
        long ts = Instant.now().getEpochSecond() - 3600; // 1 hour ago
        String secret = "whsec_test_secret";
        String signed = ts + "." + payload;
        byte[] mac = new org.apache.commons.codec.digest.HmacUtils(org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256, secret).hmac(signed);
        String expected = bytesToHex(mac);
        String header = "t=" + ts + ",v1=" + expected;

        Assertions.assertFalse(v.verify(payload, header, secret, 300L));
    }

    @Test
    void verify_wrongSignature_returnsFalse() {
        StripeWebhookVerifier v = new StripeWebhookVerifier();
        String payload = "{}";
        long ts = Instant.now().getEpochSecond();
        String secret = "whsec_test_secret";
        String header = "t=" + ts + ",v1=deadbeef";

        Assertions.assertFalse(v.verify(payload, header, secret, 300L));
    }

    @Test
    public void testVerifyKnownVector() {
        // example using secret "whsec_test"
        String secret = "whsec_test";
        String payload = "{\"id\":\"evt_test\",\"object\":\"event\"}";
        String t = String.valueOf(Instant.now().getEpochSecond());

        StripeWebhookVerifier v = new StripeWebhookVerifier();
        try {
            String signed = t + "." + payload;
            org.apache.commons.codec.digest.HmacUtils h = new org.apache.commons.codec.digest.HmacUtils(org.apache.commons.codec.digest.HmacAlgorithms.HMAC_SHA_256, secret);
            byte[] mac = h.hmac(signed);
            StringBuilder sb = new StringBuilder();
            for (byte b : mac) sb.append(String.format("%02x", b & 0xff));
            String sig = sb.toString();
            String header = "t=" + t + ",v1=" + sig;
            boolean ok = v.verify(payload, header, secret, 300L);
            Assertions.assertTrue(ok);
        } catch (Exception ex) {
            Assertions.fail("exception during test: " + ex.getMessage());
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
