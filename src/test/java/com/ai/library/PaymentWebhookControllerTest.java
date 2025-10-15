package com.ai.library;

import com.ai.library.model.Fine;
import com.ai.library.model.Payment;
import com.ai.library.repository.FineRepository;
import com.ai.library.repository.PaymentRepository;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "payment.stripe.webhook-enabled=true",
        "payment.stripe.webhook-secret=whsec_test_integration"
})
public class PaymentWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FineRepository fineRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    public void webhookMarksFinePaid_andCreatesPayment() throws Exception {
        // create fine
        Fine f = new Fine();
        f.setAmount(12.5);
        f.setReason("late");
        f.setPaid(false);
        f = fineRepository.save(f);
        final Long fineId = f.getId();

        String payload = "{\"id\":\"evt_test\",\"object\":\"event\",\"type\":\"payment_intent.succeeded\"," +
                "\"data\":{\"object\":{\"id\":\"pi_test\",\"metadata\":{\"fine_id\":\"" + fineId + "\"}}}}";

        String t = String.valueOf(java.time.Instant.now().getEpochSecond());
        String signed = t + "." + payload;
        HmacUtils h = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, "whsec_test_integration");
        byte[] mac = h.hmac(signed);
        StringBuilder sb = new StringBuilder();
        for (byte b : mac) sb.append(String.format("%02x", b & 0xff));
        String sig = sb.toString();
        String header = "t=" + t + ",v1=" + sig;

        mockMvc.perform(post("/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", header)
                        .content(payload))
                .andExpect(status().isOk());

        // reload fine and payments
        Fine re = fineRepository.findById(fineId).orElseThrow();
        Assertions.assertTrue(re.isPaid(), "Fine should be marked paid by webhook");

        List<Payment> payments = paymentRepository.findAll();
        boolean found = payments.stream().anyMatch(p -> p.getFine() != null && p.getFine().getId().equals(fineId));
        Assertions.assertTrue(found, "A Payment should have been created for the fine");
    }
}
