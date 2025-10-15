package com.ai.library;

import com.ai.library.ai.OpenAiClient;
import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import com.ai.library.service.BookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BookEmbeddingTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    public void save_should_store_embedding_json_when_ai_available() throws Exception {
        // simple test double to avoid Mockito inline-mock issues on newer JDKs
        class TestOpenAiClient extends OpenAiClient {
            @Override
            public double[] generateEmbedding(String text) {
                return new double[]{0.1, 0.2, 0.3};
            }
        }

        OpenAiClient ai = new TestOpenAiClient();
        BookService svc = new BookService(bookRepository, ai);

        Book b = new Book();
        b.setTitle("Test Book");
        b.setSummary("A short summary");
        b.setTotalCopies(1);

        Book saved = svc.save(b);
        assertThat(saved.getEmbeddingJson()).isNotNull();

        ObjectMapper om = new ObjectMapper();
        double[] emb = om.readValue(saved.getEmbeddingJson(), double[].class);
        assertThat(emb).containsExactly(0.1, 0.2, 0.3);

        Optional<Book> fromDb = bookRepository.findById(saved.getId());
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getEmbeddingJson()).isNotNull();
    }
}
