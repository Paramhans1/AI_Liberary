package com.ai.library.service;

import com.ai.library.model.Book;
import com.ai.library.model.Fine;
import com.ai.library.model.Transaction;
import com.ai.library.model.User;
import com.ai.library.repository.BookRepository;
import com.ai.library.repository.FineRepository;
import com.ai.library.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BorrowService {
    private final TransactionRepository transactionRepository;
    private final BookRepository bookRepository;
    private final FineRepository fineRepository;
    private final FinePolicyService finePolicyService;

    public BorrowService(TransactionRepository transactionRepository, BookRepository bookRepository, FineRepository fineRepository, FinePolicyService finePolicyService) {
        this.transactionRepository = transactionRepository;
        this.bookRepository = bookRepository;
        this.fineRepository = fineRepository;
        this.finePolicyService = finePolicyService;
    }

    public Transaction borrowBook(User user, Book book, int days) {
        if (book.getAvailableCopies() <= 0) throw new IllegalStateException("No copies available");
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Transaction t = new Transaction();
        t.setUser(user);
        t.setBook(book);
        t.setBorrowedAt(LocalDateTime.now());
        t.setDueAt(LocalDateTime.now().plusDays(days));
        t.setStatus(com.ai.library.model.TransactionStatus.BORROWED);
        return transactionRepository.save(t);
    }

    public Transaction returnBook(Transaction tx) {
        tx.setReturnedAt(LocalDateTime.now());
        if (tx.getDueAt().isBefore(tx.getReturnedAt())) {
            tx.setStatus(com.ai.library.model.TransactionStatus.LATE);
            long daysLate = java.time.Duration.between(tx.getDueAt(), tx.getReturnedAt()).toDays();
            double rate = finePolicyService.getPolicy().getAmountPerDay();
            double fine = daysLate * rate; // configurable policy
            tx.setFineAmount(fine);
            // persist a Fine record
            Fine f = new Fine();
            f.setUser(tx.getUser());
            f.setAmount(fine);
            f.setReason("Late return: " + daysLate + " days");
            f.setPaid(false);
            fineRepository.save(f);
        } else {
            tx.setStatus(com.ai.library.model.TransactionStatus.RETURNED);
        }
        Book b = tx.getBook();
        b.setAvailableCopies(b.getAvailableCopies() + 1);
        bookRepository.save(b);
        return transactionRepository.save(tx);
    }
}
