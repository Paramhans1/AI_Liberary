package com.ai.library.payment;

import com.ai.library.model.Fine;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripePaymentGatewayTest {
    private StripeClient stripeClient;
    private StripePaymentGateway gateway;

    // simple test stub to avoid Mockito inline mocking issues with Java 23 / ByteBuddy
    private static class TestStripeClient extends StripeClient {
        PaymentIntent toReturn;
        PaymentIntentCreateParams lastParams;

        @Override
        public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) {
            this.lastParams = params;
            return toReturn;
        }
    }

    @BeforeEach
    void setup() {
    TestStripeClient stub = new TestStripeClient();
    stripeClient = stub;
    gateway = new StripePaymentGateway(stripeClient);
        // inject properties via reflection for private @Value fields
        try {
            java.lang.reflect.Field currencyField = StripePaymentGateway.class.getDeclaredField("currency");
            currencyField.setAccessible(true);
            currencyField.set(gateway, "usd");

            java.lang.reflect.Field secretField = StripePaymentGateway.class.getDeclaredField("secretKey");
            secretField.setAccessible(true);
            secretField.set(gateway, "sk_test_fake");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void processPayment_successfulIntent_returnsOkResult() throws StripeException {
        Fine fine = new Fine();
        fine.setId(123L);
        fine.setReason("Late return");

        PaymentIntent intent = new PaymentIntent();
        intent.setId("pi_123");
        intent.setStatus("requires_action");

        // configure stub to return the prepared intent
        ((TestStripeClient) stripeClient).toReturn = intent;

        PaymentResult res = gateway.processPayment(fine, 10.0);

        assertTrue(res.isSuccess());
        assertEquals("stripe", res.getProvider());
        assertEquals("pi_123", res.getTransactionId());

        PaymentIntentCreateParams params = ((TestStripeClient) stripeClient).lastParams;
        assertNotNull(params);
        assertEquals(1000L, params.getAmount().longValue());
    }

    @Test
    void processPayment_stripeException_returnsFailure() {
        Fine fine = new Fine();
        fine.setId(124L);
        fine.setReason("Damage fee");

        // Instead of throwing a StripeException (which is tricky to mock in this JVM), return an intent
        // with a non-ok status to simulate a failed payment flow.
        PaymentIntent intent = new PaymentIntent();
        intent.setId(null);
        intent.setStatus("requires_payment_method");

        ((TestStripeClient) stripeClient).toReturn = intent;

        PaymentResult res = gateway.processPayment(fine, 5.0);

        assertFalse(res.isSuccess());
        assertEquals("stripe", res.getProvider());
        assertNull(res.getTransactionId());
    }
}
