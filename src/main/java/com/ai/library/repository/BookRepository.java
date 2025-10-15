package com.ai.library.repository;

import com.ai.library.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);

    @org.springframework.data.jpa.repository.Query("select b.id, count(t.id) as cnt from Book b left join Transaction t on t.book = b group by b.id order by cnt desc")
    java.util.List<Object[]> findBorrowCountsPerBook();
}
