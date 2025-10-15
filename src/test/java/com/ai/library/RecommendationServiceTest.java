package com.ai.library;

import com.ai.library.ai.OpenAiClient;
import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import com.ai.library.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

@DataJpaTest
public class RecommendationServiceTest {

    @Autowired
    private BookRepository bookRepository;

    @TestConfiguration
    static class Config {
        @Bean
        public OpenAiClient openAiClient() {
            // return a stub that generates simple embeddings based on ASCII of title
            return new OpenAiClient() {
                @Override
                public double[] generateEmbedding(String text) {
                    double[] out = new double[8];
                    for (int i = 0; i < out.length; i++) out[i] = 0.0;
                    if (text == null) return out;
                    for (int i = 0; i < text.length() && i < out.length; i++) {
                        out[i] = (double) text.charAt(i) / 255.0;
                    }
                    return out;
                }

                @Override
                public String chat(String prompt) { return "stub"; }
            };
        }

        @Bean
        public ObjectMapper objectMapper() { return new ObjectMapper(); }

        @Bean
        public RecommendationService recommendationService(BookRepository bookRepository, OpenAiClient client, ObjectMapper mapper) {
            return new RecommendationService(bookRepository, client, mapper);
        }
    }

    @Test
    public void recommendsSimilarBooks() throws Exception {
        Book a = new Book(); a.setTitle("Java Programming"); a.setSummary("Learn Java"); a = bookRepository.save(a);
        Book b = new Book(); b.setTitle("Advanced Java"); b.setSummary("Deep Java topics"); b = bookRepository.save(b);
        Book c = new Book(); c.setTitle("Python Cookbook"); c.setSummary("Recipes for Python"); c = bookRepository.save(c);

        RecommendationService svc = new RecommendationService(bookRepository, new Config().openAiClient(), new Config().objectMapper());
        svc.ensureEmbeddingsForAllBooks();

        List<RecommendationService.Recommendation> recs = svc.recommendForBook(a.getId(), 5);
        // Expect 'b' (Advanced Java) to be more similar to 'a' (Java Programming) than 'c'
        Assertions.assertFalse(recs.isEmpty(), "should return some recommendations");
        Assertions.assertEquals(b.getId(), recs.get(0).getId(), "closest should be b");
    }
}
