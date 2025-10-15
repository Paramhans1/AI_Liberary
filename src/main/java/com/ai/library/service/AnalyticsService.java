package com.ai.library.service;

import com.ai.library.repository.TransactionRepository;
import com.ai.library.repository.BookRepository;
import com.ai.library.repository.UserRepository;
import com.ai.library.repository.FineRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.time.LocalDateTime;

@Service
public class AnalyticsService {
    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final FineRepository fineRepository;

    public AnalyticsService(TransactionRepository transactionRepository, BookRepository bookRepository, UserRepository userRepository, FineRepository fineRepository) {
        this.transactionRepository = transactionRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.fineRepository = fineRepository;
    }

    public Map<String, Object> getOverview() {
        long totalBooks = bookRepository.count();
        long totalUsers = userRepository.count();
        long totalTransactions = transactionRepository.count();

        long currentlyBorrowed = transactionRepository.countByStatus(com.ai.library.model.TransactionStatus.BORROWED);
        List<com.ai.library.model.Transaction> overdue = transactionRepository.findOverdue(LocalDateTime.now());

        double totalOutstandingFines = fineRepository.findAll().stream()
                .filter(f -> !f.isPaid())
                .mapToDouble(f -> f.getAmount())
                .sum();

        // top borrowed books (ids -> count)
        List<Object[]> borrowCounts = bookRepository.findBorrowCountsPerBook();
        List<Map<String,Object>> topBooks = borrowCounts.stream().limit(5).map(row -> Map.of("bookId", row[0], "count", ((Number)row[1]).longValue())).collect(Collectors.toList());

        return Map.of(
                "totalBooks", totalBooks,
                "totalUsers", totalUsers,
                "totalTransactions", totalTransactions,
                "currentlyBorrowed", currentlyBorrowed,
                "overdueCount", overdue.size(),
                "totalOutstandingFines", totalOutstandingFines,
                "topBorrowedBooks", topBooks
        );
    }
}
