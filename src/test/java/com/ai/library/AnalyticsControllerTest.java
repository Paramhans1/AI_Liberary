package com.ai.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AnalyticsControllerTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void overviewReturnsKeys() {
        // register a user and login to obtain JWT (endpoint requires authentication)
        com.ai.library.dto.AuthRequest req = new com.ai.library.dto.AuthRequest();
        req.setUsername("analyticstester");
        req.setPassword("pass");

        ResponseEntity<String> reg = rest.postForEntity("/api/auth/register", req, String.class);
        assertThat(reg.getStatusCode().is2xxSuccessful()).isTrue();

        ResponseEntity<com.ai.library.dto.AuthResponse> login = rest.postForEntity("/api/auth/login", req, com.ai.library.dto.AuthResponse.class);
        assertThat(login.getStatusCode().is2xxSuccessful()).isTrue();
        String token = login.getBody().getToken();
        assertThat(token).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(null, headers);
        ResponseEntity<Map> resp = rest.exchange("/api/analytics/overview", HttpMethod.GET, entity, Map.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Map body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKeys("totalBooks", "totalUsers", "currentlyBorrowed", "overdueCount", "topBorrowedBooks");
    }
}
