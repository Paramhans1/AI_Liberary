package com.ai.library.payment;

import com.ai.library.model.Fine;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StubPaymentGateway implements PaymentGateway {
    @Override
    public PaymentResult processPayment(Fine fine, double amount) {
        // very simple stub: accept if amount >= fine.amount
        boolean ok = amount >= fine.getAmount();
        String tx = UUID.randomUUID().toString();
        return new PaymentResult(ok, "stub", tx, ok ? "OK" : "insufficient");
    }
}
