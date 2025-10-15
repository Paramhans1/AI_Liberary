package com.ai.library.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    @Test
    public void generateAndValidateToken() {
        JwtUtil jwtUtil = new JwtUtil();
        // generate a secure HS512 key and set it as base64 so it meets algorithm requirements
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String b64 = Base64.getEncoder().encodeToString(key.getEncoded());
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "base64:" + b64);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 3600000L);

        String token = jwtUtil.generateToken("alice", Set.of("ROLE_STUDENT"));
        assertNotNull(token);
        assertTrue(jwtUtil.validateJwtToken(token));
        String username = jwtUtil.getUsernameFromToken(token);
        assertEquals("alice", username);
        var roles = jwtUtil.getRolesFromToken(token);
        assertTrue(roles.contains("ROLE_STUDENT"));
    }
}
