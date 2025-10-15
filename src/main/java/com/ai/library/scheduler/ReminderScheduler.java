package com.ai.library.scheduler;

import com.ai.library.model.Transaction;
import com.ai.library.repository.TransactionRepository;
import com.ai.library.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    public ReminderScheduler(TransactionRepository transactionRepository, EmailService emailService) {
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
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
}
