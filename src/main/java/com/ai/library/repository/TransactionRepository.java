package com.ai.library.repository;

import com.ai.library.model.Transaction;
import com.ai.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);

    // count currently borrowed transactions (status BORROWED)
    long countByStatus(com.ai.library.model.TransactionStatus status);

    // find overdue transactions where returnedAt is null and dueAt < now
    @org.springframework.data.jpa.repository.Query("select t from Transaction t where t.returnedAt is null and t.dueAt < :now")
    java.util.List<com.ai.library.model.Transaction> findOverdue(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
