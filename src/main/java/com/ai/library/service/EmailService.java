package com.ai.library.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final Counter sentCounter;
    private final Counter failedCounter;

    public EmailService(JavaMailSender mailSender, MeterRegistry meterRegistry) {
        this.mailSender = mailSender;
        this.sentCounter = Counter.builder("email.sent.count").description("Number of emails successfully sent").register(meterRegistry);
        this.failedCounter = Counter.builder("email.failed.count").description("Number of email send failures").register(meterRegistry);
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);

        int attempts = 0;
        int maxAttempts = 3;
        while (attempts < maxAttempts) {
            try {
                mailSender.send(msg);
                sentCounter.increment();
                return;
            } catch (Exception ex) {
                attempts++;
                failedCounter.increment();
                try { Thread.sleep(500L * attempts); } catch (InterruptedException ignored) {}
            }
        }
        // final fallback log
        System.err.println("Failed to send email to " + to + " after " + maxAttempts + " attempts");
    }
}
