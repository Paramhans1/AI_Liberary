package com.ai.library;

import com.ai.library.ai.OpenAiClient;
import com.ai.library.controller.SemanticSearchController;
import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class SemanticSearchTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    public void semantic_search_ranks_books_by_similarity() throws Exception {
        // Prepare two books with distinct embeddings
        Book b1 = new Book();
        b1.setTitle("Alpha");
        b1.setSummary("First");
        b1.setTotalCopies(1);
        b1.setEmbeddingJson(new ObjectMapper().writeValueAsString(new double[]{1.0, 0.0, 0.0}));
        bookRepository.save(b1);

        Book b2 = new Book();
        b2.setTitle("Beta");
        b2.setSummary("Second");
        b2.setTotalCopies(1);
        b2.setEmbeddingJson(new ObjectMapper().writeValueAsString(new double[]{0.0, 1.0, 0.0}));
        bookRepository.save(b2);

        // Test double for OpenAiClient to avoid Mockito inline-mock issues on Java 23
        class TestOpenAiClient extends OpenAiClient {
            @Override
            public double[] generateEmbedding(String text) {
                return new double[]{0.0, 0.9, 0.0};
            }
        }

        OpenAiClient ai = new TestOpenAiClient();

        SemanticSearchController ctrl = new SemanticSearchController(ai, bookRepository);
        ResponseEntity<?> resp = ctrl.semanticSearch("some query", 10);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Object body = resp.getBody();
        assertThat(body).isInstanceOf(List.class);
        List<?> list = (List<?>) body;
        assertThat(list).isNotEmpty();
        // top result should be book b2 (Beta)
        Object first = list.get(0);
        assertThat(first).isInstanceOf(java.util.Map.class);
        java.util.Map<?,?> m = (java.util.Map<?,?>) first;
        Object bookObj = m.get("book");
        assertThat(bookObj).isInstanceOf(Book.class);
        Book top = (Book) bookObj;
        assertThat(top.getTitle()).isEqualTo("Beta");
    }
}
