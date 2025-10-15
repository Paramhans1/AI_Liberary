package com.ai.library.controller;

import com.ai.library.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/pay/fine")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> payFine(@RequestParam Long fineId, @RequestParam double amount) {
        try {
            var paid = paymentService.payFine(fineId, amount);
            return ResponseEntity.ok(paid);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
