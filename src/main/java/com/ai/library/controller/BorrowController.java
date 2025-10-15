package com.ai.library.controller;

import com.ai.library.model.Transaction;
import com.ai.library.model.User;
import com.ai.library.repository.BookRepository;
import com.ai.library.repository.TransactionRepository;
import com.ai.library.repository.UserRepository;
import com.ai.library.service.BorrowService;
import com.ai.library.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {
    private final BorrowService borrowService;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final TransactionRepository transactionRepository;
    private final EmailService emailService;

    public BorrowController(BorrowService borrowService, UserRepository userRepository, BookRepository bookRepository, TransactionRepository transactionRepository, EmailService emailService) {
        this.borrowService = borrowService;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
    }

    private Optional<User> currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Optional.empty();
        Object principal = auth.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails) username = ((UserDetails) principal).getUsername();
        else if (principal instanceof String) username = (String) principal;
        if (username == null) return Optional.empty();
        return userRepository.findByUsername(username);
    }

    @PostMapping("")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> borrow(@RequestParam Long bookId, @RequestParam(defaultValue = "14") int days) {
        var userOpt = currentUser();
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("Unauthorized");
        var bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isEmpty()) return ResponseEntity.badRequest().body("Book not found");
        try {
            Transaction tx = borrowService.borrowBook(userOpt.get(), bookOpt.get(), days);
            // send confirmation email (best-effort)
            if (userOpt.get().getEmail() != null && !userOpt.get().getEmail().isBlank()) {
                emailService.sendSimpleMessage(userOpt.get().getEmail(), "Borrow Confirmation",
                        "You borrowed '" + bookOpt.get().getTitle() + "'. Due: " + tx.getDueAt());
            }
            return ResponseEntity.ok(tx);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('STUDENT','LIBRARIAN','ADMIN')")
    public ResponseEntity<?> returnBook(@RequestParam Long transactionId) {
        var userOpt = currentUser();
        if (userOpt.isEmpty()) return ResponseEntity.status(401).body("Unauthorized");
        var txOpt = transactionRepository.findById(transactionId);
        if (txOpt.isEmpty()) return ResponseEntity.badRequest().body("Transaction not found");
        Transaction tx = txOpt.get();
        if (!tx.getUser().getId().equals(userOpt.get().getId())) return ResponseEntity.status(403).body("Not your transaction");
        Transaction updated = borrowService.returnBook(tx);
        if (userOpt.get().getEmail() != null && !userOpt.get().getEmail().isBlank()) {
            emailService.sendSimpleMessage(userOpt.get().getEmail(), "Return Confirmation",
                    "You returned '" + tx.getBook().getTitle() + "'. Status: " + updated.getStatus() + ", Fine: " + updated.getFineAmount());
        }
        return ResponseEntity.ok(updated);
    }
}
