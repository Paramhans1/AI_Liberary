package com.ai.library.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Component;

/**
 * Lightweight wrapper around Stripe SDK to centralize SDK usage and make it mockable in tests.
 */
@Component
public class StripeClient {
    public void init(String apiKey) {
        Stripe.apiKey = apiKey;
    }

    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return PaymentIntent.create(params);
    }
}
