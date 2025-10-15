package com.ai.library.payment;

import com.ai.library.model.Fine;

public interface PaymentGateway {
    PaymentResult processPayment(Fine fine, double amount);
}
