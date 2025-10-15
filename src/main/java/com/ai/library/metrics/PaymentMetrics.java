package com.ai.library.metrics;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class PaymentMetrics {
    private final AtomicLong paymentsReceived = new AtomicLong();
    private final AtomicLong paymentsSaved = new AtomicLong();
    private final AtomicLong duplicatesIgnored = new AtomicLong();

    public void incReceived() { paymentsReceived.incrementAndGet(); }
    public void incSaved() { paymentsSaved.incrementAndGet(); }
    public void incDuplicatesIgnored() { duplicatesIgnored.incrementAndGet(); }

    public long getPaymentsReceived() { return paymentsReceived.get(); }
    public long getPaymentsSaved() { return paymentsSaved.get(); }
    public long getDuplicatesIgnored() { return duplicatesIgnored.get(); }
}
