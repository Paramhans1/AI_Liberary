package com.ai.library.controller;

import com.ai.library.metrics.PaymentMetrics;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/metrics")
public class AdminMetricsController {
    private final PaymentMetrics metrics;
    public AdminMetricsController(PaymentMetrics metrics) { this.metrics = metrics; }

    @GetMapping("/payments")
    public ResponseEntity<?> payments() {
        return ResponseEntity.ok(Map.of(
                "received", metrics.getPaymentsReceived(),
                "saved", metrics.getPaymentsSaved(),
                "duplicatesIgnored", metrics.getDuplicatesIgnored()
        ));
    }
}
