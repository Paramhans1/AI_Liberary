package com.ai.library.controller;

import com.ai.library.model.Fine;
import com.ai.library.model.Payment;
import com.ai.library.repository.FineRepository;
import com.ai.library.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ai.library.payment.StripeWebhookVerifier;
import com.ai.library.metrics.PaymentMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Minimal webhook handler for Stripe events. Optional — enabled when payment.stripe.enabled=true.
 */
@RestController
@RequestMapping("/webhooks")
@ConditionalOnProperty(prefix = "payment.stripe", name = "webhook-enabled", havingValue = "true")
public class PaymentWebhookController {
    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final ObjectMapper objectMapper;
    private final FineRepository fineRepository;
    private final PaymentRepository paymentRepository;
    private final StripeWebhookVerifier verifier;
    private final com.ai.library.config.PaymentProperties paymentProperties;
    private final PaymentMetrics paymentMetrics;

    public PaymentWebhookController(FineRepository fineRepository, PaymentRepository paymentRepository, StripeWebhookVerifier verifier, ObjectMapper objectMapper, com.ai.library.config.PaymentProperties paymentProperties, PaymentMetrics paymentMetrics) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.verifier = verifier;
        this.objectMapper = objectMapper;
        this.paymentProperties = paymentProperties;
        this.paymentMetrics = paymentMetrics;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripe(@RequestBody String payload, @RequestHeader(name = "Stripe-Signature", required = false) String sig, @RequestHeader(name = "Content-Type", required = false) String contentType) {
        try {
            // If a webhook secret is configured via properties, verify signature with a 5-minute tolerance
            String webhookSecret = paymentProperties.getWebhookSecret();
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                long tol = paymentProperties.getWebhookToleranceSeconds() <= 0 ? 300 : paymentProperties.getWebhookToleranceSeconds();
                boolean ok = verifier.verify(payload, sig, webhookSecret, tol);
                if (!ok) {
                    log.warn("Stripe webhook signature verification failed");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid signature");
                }
            } else {
                log.debug("No stripe webhook secret configured; skipping signature verification");
            }

            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText();
            JsonNode data = root.path("data").path("object");

            if ("payment_intent.succeeded".equals(type) || "charge.succeeded".equals(type)) {
                String metadataFineId = data.path("metadata").path("fine_id").asText(null);
                if (metadataFineId == null || metadataFineId.isEmpty()) {
                    log.info("Stripe webhook succeeded but no fine_id metadata present");
                    return ResponseEntity.ok("ignored");
                }

                Long fineId = Long.valueOf(metadataFineId);
                var fOpt = fineRepository.findById(fineId);
                if (fOpt.isEmpty()) {
                    log.warn("Webhook payment for unknown fine id={}", fineId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("fine not found");
                }

                Fine f = fOpt.get();
                String txId = data.path("id").asText(null);
                // idempotency: if this transactionId already exists, ignore
                    paymentMetrics.incReceived();
                    if (txId != null && !txId.isBlank()) {
                        boolean already = paymentRepository.findByTransactionId(txId).isPresent();
                        if (already) {
                            log.info("Duplicate webhook for transaction {} - ignoring", txId);
                            paymentMetrics.incDuplicatesIgnored();
                            return ResponseEntity.ok("ignored-duplicate");
                        }
                    }

                    if (!f.isPaid()) {
                        f.setPaid(true);
                        fineRepository.save(f);

                        Payment p = new Payment();
                        p.setFine(f);
                        p.setAmount(f.getAmount());
                        p.setMethod("stripe");
                        p.setTransactionId(txId);
                        p.setPaidAt(LocalDateTime.now());
                        try {
                            paymentRepository.save(p);
                            paymentMetrics.incSaved();
                        } catch (DataIntegrityViolationException dive) {
                            // This can happen in a rare race where another webhook/process created the same transaction id.
                            // Log and treat as an ignored duplicate to keep handler idempotent and resilient.
                            log.info("Payment save failed due to integrity violation (likely duplicate tx): {} - treating as duplicate", txId);
                            paymentMetrics.incDuplicatesIgnored();
                        }
                    }
                return ResponseEntity.ok("processed");
            }

            return ResponseEntity.ok("ignored");
        } catch (Exception ex) {
            log.error("Error handling stripe webhook", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }
}
