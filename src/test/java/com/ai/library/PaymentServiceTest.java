package com.ai.library;

import com.ai.library.model.Fine;
import com.ai.library.model.Payment;
import com.ai.library.payment.PaymentGateway;
import com.ai.library.payment.PaymentResult;
import com.ai.library.repository.FineRepository;
import com.ai.library.repository.PaymentRepository;
import com.ai.library.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class PaymentServiceTest {

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    public void payFine_records_payment_when_gateway_success() {
    com.ai.library.model.User u = new com.ai.library.model.User();
    u.setUsername("testuser");
    u.setPassword("x");
    // not saving user explicitly; set the association on fine
    Fine f = new Fine();
    f.setAmount(50.0);
    f.setPaid(false);
    f.setUser(u);
    fineRepository.save(f);

        // test gateway
        PaymentGateway gw = new PaymentGateway() {
            @Override
            public PaymentResult processPayment(com.ai.library.model.Fine fine, double amount) {
                return new PaymentResult(true, "test-gw", "tx-123", "ok");
            }
        };

        PaymentService svc = new PaymentService(fineRepository, paymentRepository, gw);
        Payment p = svc.payFine(f.getId(), 50.0);
        assertThat(p).isNotNull();
        assertThat(p.getTransactionId()).isEqualTo("tx-123");
        assertThat(p.getMethod()).isEqualTo("test-gw");

        var updated = fineRepository.findById(f.getId()).get();
        assertThat(updated.isPaid()).isTrue();
    }
}
