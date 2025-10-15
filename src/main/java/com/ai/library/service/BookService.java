package com.ai.library.service;

import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import com.ai.library.ai.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.Nullable;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Single constructor. OpenAiClient is optional for environments without AI configuration.
    public BookService(BookRepository bookRepository, @Nullable OpenAiClient openAiClient) {
        this.bookRepository = bookRepository;
        this.openAiClient = openAiClient;
    }

    public Book save(Book book) {
        if (book.getAvailableCopies() == null) book.setAvailableCopies(book.getTotalCopies());
        // compute embedding if OpenAiClient available
        try {
            if (openAiClient != null) {
                String txt = (book.getTitle() == null ? "" : book.getTitle()) + "\n" + (book.getSummary() == null ? "" : book.getSummary()) + "\n" + (book.getTags() == null ? "" : book.getTags());
                double[] emb = openAiClient.generateEmbedding(txt);
                if (emb != null && emb.length > 0) {
                    String json = objectMapper.writeValueAsString(emb);
                    book.setEmbeddingJson(json);
                }
            }
        } catch (Exception e) {
            // log then continue
            System.err.println("Failed to compute embedding: " + e.getMessage());
        }
        return bookRepository.save(book);
    }

    public BookRepository getRepository() {
        return this.bookRepository;
    }

    public java.util.Optional<Book> findById(Long id) {
        return this.bookRepository.findById(id);
    }

    public Book updateEbookPath(Long id, String path) {
        var ob = findById(id);
        if (ob.isEmpty()) return null;
        var b = ob.get();
        b.setEbookPath(path);
        return save(b);
    }

    public List<Book> search(String q) {
        return bookRepository.findByTitleContainingIgnoreCase(q);
    }
}
