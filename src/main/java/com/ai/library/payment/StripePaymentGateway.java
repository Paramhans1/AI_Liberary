package com.ai.library.payment;

import com.ai.library.model.Fine;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Minimal Stripe gateway implementation using Stripe HTTP API.
 * This bean is conditional on property payment.stripe.enabled=true so it is optional.
 */
@Component
@ConditionalOnProperty(prefix = "payment.stripe", name = "enabled", havingValue = "true")
public class StripePaymentGateway implements PaymentGateway {
    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);
    @Value("${payment.stripe.secretKey:}")
    private String secretKey;

    @Value("${payment.stripe.currency:usd}")
    private String currency;

    private final StripeClient stripeClient;

    public StripePaymentGateway(StripeClient stripeClient) {
        this.stripeClient = stripeClient;
    }

    @Override
    public PaymentResult processPayment(Fine fine, double amount) {
        if (secretKey == null || secretKey.isBlank()) {
            return new PaymentResult(false, "stripe", null, "stripe secret key not configured");
        }
        try {
        // Initialize Stripe SDK via wrapper
        stripeClient.init(secretKey);

        long amountCents = Math.round(amount * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(amountCents)
            .setCurrency(currency)
            .addPaymentMethodType("card")
            .setDescription("Library fine: " + fine.getReason())
            .putMetadata("fine_id", String.valueOf(fine.getId()))
            .build();

        PaymentIntent intent = stripeClient.createPaymentIntent(params);

            String id = intent.getId();
            String status = intent.getStatus();
            boolean ok = "succeeded".equalsIgnoreCase(status) || "requires_capture".equalsIgnoreCase(status) || "requires_action".equalsIgnoreCase(status);
            String message = status;

            return new PaymentResult(ok, "stripe", id, message);
        } catch (StripeException ex) {
            log.error("Stripe SDK error", ex);
            return new PaymentResult(false, "stripe", null, ex.getMessage());
        }
    }
}
