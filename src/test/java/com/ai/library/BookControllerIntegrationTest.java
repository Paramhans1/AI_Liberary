package com.ai.library;

import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    public void uploadAndDownloadEbook_shouldWork() throws Exception {
    Book b = new Book();
    b.setTitle("TestBook");
    b.setAuthor("Author");
    Book saved = bookRepository.save(b);
    final Long id = saved.getId();

        byte[] content = "PDF-DATA".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", content);

        var uploadRes = mvc.perform(multipart("/api/books/upload/" + id).file(file))
            .andExpect(status().isOk())
            .andReturn();

        // verify book updated
    var updated = bookRepository.findById(id).orElseThrow();
        Assertions.assertNotNull(updated.getEbookPath());

        // download
        mvc.perform(get("/api/books/download/" + id))
            .andExpect(status().isOk())
            .andExpect(header().exists("Content-Disposition"));

        // clean up file
        Path p = Path.of(updated.getEbookPath());
        if (Files.exists(p)) Files.delete(p);
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    public void uploadInvalidExtension_shouldFail() throws Exception {
    Book b = new Book();
    b.setTitle("TestBook2");
    b.setAuthor("Author");
    Book saved2 = bookRepository.save(b);
    final Long id2 = saved2.getId();

        byte[] content = "NOT-PDF".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", content);
        // the service throws a RuntimeException for unsupported type; assert it bubbles up
        Exception ex = Assertions.assertThrows(Exception.class, () -> {
            mvc.perform(multipart("/api/books/upload/" + id2).file(file)).andReturn();
        });
        String msg = ex.getMessage();
        Assertions.assertTrue(msg.contains("Unsupported file type") || (ex.getCause() != null && ex.getCause().getMessage().contains("Unsupported file type")));
    }
}
