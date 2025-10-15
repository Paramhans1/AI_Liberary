package com.ai.library.scheduler;

import com.ai.library.model.Transaction;
import com.ai.library.repository.TransactionRepository;
import com.ai.library.service.EmailService;
import com.ai.library.repository.FineRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;
    private final FineRepository fineRepository;
    private final Counter finesCreatedCounter;

    public ReminderScheduler(TransactionRepository transactionRepository, EmailService emailService, FineRepository fineRepository, MeterRegistry meterRegistry) {
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
        this.fineRepository = fineRepository;
        this.finesCreatedCounter = Counter.builder("fines.created.count").description("Number of fines created by scheduler").register(meterRegistry);
    }

    // Runs every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDueSoonReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime inTwoDays = now.plusDays(2);
        List<Transaction> all = transactionRepository.findAll();
        for (Transaction t : all) {
            if (t.getStatus() == com.ai.library.model.TransactionStatus.BORROWED && t.getDueAt() != null) {
                if (t.getUser() != null && t.getUser().getEmail() != null && !t.getUser().getEmail().isBlank()
                        && t.getDueAt().isAfter(now) && t.getDueAt().isBefore(inTwoDays)) {
                    emailService.sendSimpleMessage(t.getUser().getEmail(), "Upcoming due date",
                            "Reminder: '" + t.getBook().getTitle() + "' is due on " + t.getDueAt());
                }
            }
        }
    }

    // Runs every day at 01:00 AM to create fines for overdue transactions
    @Scheduled(cron = "0 0 1 * * *")
    public void createFinesForOverdue() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.util.List<Transaction> overdue = transactionRepository.findOverdue(now);
        for (Transaction t : overdue) {
            if (t.getUser() != null) {
                // simple fine: amount = 1.0 per day overdue
                long daysOverdue = java.time.Duration.between(t.getDueAt(), now).toDays();
                if (daysOverdue < 1) daysOverdue = 1;
                double amount = daysOverdue * 1.0;
                com.ai.library.model.Fine fine = new com.ai.library.model.Fine();
                fine.setUser(t.getUser());
                fine.setAmount(amount);
                fine.setReason("Overdue book: " + (t.getBook() != null ? t.getBook().getTitle() : "unknown"));
                fine.setPaid(false);
                fineRepository.save(fine);
                finesCreatedCounter.increment();
                // notify user
                if (t.getUser().getEmail() != null && !t.getUser().getEmail().isBlank()) {
                    emailService.sendSimpleMessage(t.getUser().getEmail(), "Overdue fine issued",
                            "A fine of " + amount + " has been issued for overdue book: " + (t.getBook() != null ? t.getBook().getTitle() : "unknown"));
                }
            }
        }
    }
}
