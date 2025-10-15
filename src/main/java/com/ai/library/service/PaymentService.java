package com.ai.library.service;

import com.ai.library.model.Fine;
import com.ai.library.model.Payment;
import com.ai.library.payment.PaymentGateway;
import com.ai.library.payment.PaymentResult;
import com.ai.library.repository.FineRepository;
import com.ai.library.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final FineRepository fineRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentService(FineRepository fineRepository, PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.fineRepository = fineRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    public Payment payFine(Long fineId, double amount) {
        var fOpt = fineRepository.findById(fineId);
        if (fOpt.isEmpty()) throw new IllegalArgumentException("fine not found");
        Fine f = fOpt.get();
        if (f.isPaid()) throw new IllegalArgumentException("fine already paid");

        PaymentResult res = paymentGateway.processPayment(f, amount);
        if (!res.isSuccess()) throw new IllegalArgumentException("payment failed: " + res.getMessage());

        f.setPaid(true);
        fineRepository.save(f);

        Payment p = new Payment();
        p.setFine(f);
        p.setAmount(amount);
        p.setMethod(res.getProvider());
        p.setTransactionId(res.getTransactionId());
        p.setPaidAt(java.time.LocalDateTime.now());
        return paymentRepository.save(p);
    }
}
