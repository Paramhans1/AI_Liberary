package com.ai.library.service;

import com.ai.library.ai.OpenAiClient;
import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final BookRepository bookRepository;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public RecommendationService(BookRepository bookRepository, OpenAiClient openAiClient, ObjectMapper objectMapper) {
        this.bookRepository = bookRepository;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
    }

    public void ensureEmbeddingsForAllBooks() {
        List<Book> all = bookRepository.findAll();
        for (Book b : all) {
            if (b.getEmbeddingJson() == null || b.getEmbeddingJson().isBlank()) {
                try {
                    String text = (b.getTitle() == null ? "" : b.getTitle()) + " " + (b.getSummary() == null ? "" : b.getSummary()) + " " + (b.getTags() == null ? "" : b.getTags());
                    double[] emb = openAiClient.generateEmbedding(text);
                    if (emb != null && emb.length > 0) {
                        String json = objectMapper.writeValueAsString(emb);
                        b.setEmbeddingJson(json);
                        bookRepository.save(b);
                    } else {
                        log.debug("No embedding produced for book {}", b.getId());
                    }
                } catch (Exception ex) {
                    log.warn("Failed to compute embedding for book {}: {}", b.getId(), ex.getMessage());
                }
            }
        }
    }

    public List<Recommendation> recommendForBook(Long bookId, int topN) {
        var opt = bookRepository.findById(bookId);
        if (opt.isEmpty()) return List.of();
        Book src = opt.get();

        double[] srcEmb = readEmbedding(src);
        if (srcEmb.length == 0) {
            // attempt to generate for source only
            try {
                String text = (src.getTitle() == null ? "" : src.getTitle()) + " " + (src.getSummary() == null ? "" : src.getSummary()) + " " + (src.getTags() == null ? "" : src.getTags());
                srcEmb = openAiClient.generateEmbedding(text);
                if (srcEmb != null && srcEmb.length > 0) {
                    src.setEmbeddingJson(objectMapper.writeValueAsString(srcEmb));
                    bookRepository.save(src);
                }
            } catch (Exception ex) {
                log.warn("Could not generate embedding for source book {}: {}", bookId, ex.getMessage());
            }
        }

        if (srcEmb == null || srcEmb.length == 0) return List.of();

        List<Book> others = bookRepository.findAll().stream().filter(b -> !b.getId().equals(bookId)).collect(Collectors.toList());
        List<Recommendation> recs = new ArrayList<>();
        for (Book b : others) {
            double[] emb = readEmbedding(b);
            if (emb == null || emb.length == 0) continue;
            double sim = cosineSimilarity(srcEmb, emb);
            recs.add(new Recommendation(b.getId(), b.getTitle(), sim));
        }

        return recs.stream().sorted(Comparator.comparingDouble(Recommendation::getScore).reversed()).limit(topN).collect(Collectors.toList());
    }

    private double[] readEmbedding(Book b) {
        try {
            if (b.getEmbeddingJson() == null) return new double[0];
            return objectMapper.readValue(b.getEmbeddingJson(), double[].class);
        } catch (Exception ex) {
            log.warn("Failed to parse embedding for book {}: {}", b.getId(), ex.getMessage());
            return new double[0];
        }
    }

    private double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null) return 0.0;
        int n = Math.min(a.length, b.length);
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < n; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    public static class Recommendation {
        private Long id;
        private String title;
        private double score;

        public Recommendation(Long id, String title, double score) {
            this.id = id;
            this.title = title;
            this.score = score;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public double getScore() { return score; }
    }
}
