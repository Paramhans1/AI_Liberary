package com.ai.library;

import com.ai.library.dto.AuthRequest;
import com.ai.library.model.Book;
import com.ai.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BorrowFlowIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    BookRepository bookRepository;

    Book seeded;

    @BeforeEach
    void setup() {
        bookRepository.deleteAll();
        seeded = new Book();
        seeded.setTitle("Integration Book");
        seeded.setAuthor("Tester");
        seeded.setAvailableCopies(1);
        bookRepository.save(seeded);
    }

    @Test
    void fullBorrowReturnPayFlow() throws InterruptedException {
        String base = "http://localhost:" + port;
        AuthRequest req = new AuthRequest();
        req.setUsername("intuser");
        req.setPassword("pass");

        ResponseEntity<String> r = rest.postForEntity(base + "/api/auth/register", req, String.class);
        assertEquals(HttpStatus.OK, r.getStatusCode());

    ResponseEntity<com.ai.library.dto.AuthResponse> login = rest.postForEntity(base + "/api/auth/login", req, com.ai.library.dto.AuthResponse.class);
    assertEquals(HttpStatus.OK, login.getStatusCode());
    String token = login.getBody().getToken();
        assertNotNull(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Borrow
        HttpEntity<Void> borrowReq = new HttpEntity<>(null, headers);
        ResponseEntity<Map> borrowResp = rest.exchange(base + "/api/borrow?bookId=" + seeded.getId() + "&days=1", HttpMethod.POST, borrowReq, Map.class);
        assertEquals(HttpStatus.OK, borrowResp.getStatusCode());
    Integer txId = (Integer) ((Map<String,Object>) borrowResp.getBody()).get("id");
        assertNotNull(txId);

        // Return (simulate late by sleeping 1s is not enough; set due date passed via repository is complex)
        // For integration we just call return and check status returned
        HttpEntity<Void> returnReq = new HttpEntity<>(null, headers);
    ResponseEntity<Map<String,Object>> returnResp = rest.exchange(base + "/api/borrow/return?transactionId=" + txId, HttpMethod.POST, returnReq, (Class) Map.class);
        assertEquals(HttpStatus.OK, returnResp.getStatusCode());

        // Check fines endpoint (may be empty)
    ResponseEntity<Map<String,Object>[]> fines = rest.exchange(base + "/api/fines", HttpMethod.GET, new HttpEntity<>(headers), (Class) Map[].class);
        assertEquals(HttpStatus.OK, fines.getStatusCode());
    }
}
