package com.ai.library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        String secret = jwtSecret;
        if (secret == null || secret.isBlank()) {
            String env = System.getenv("SECURITY_JWT_SECRET");
            if (env != null && !env.isBlank()) {
                secret = env;
            }
        }

        if (secret == null || secret.isBlank()) {
            // generate ephemeral key for development if nothing is configured
            logger.warn("No JWT secret configured (security.jwt.secret or SECURITY_JWT_SECRET); generating an ephemeral key for development. Tokens will not survive restarts.");
            return Keys.secretKeyFor(SignatureAlgorithm.HS512);
        }

        byte[] keyBytes;
        if (secret.startsWith("base64:")) {
            String b = secret.substring("base64:".length());
            keyBytes = Base64.getDecoder().decode(b);
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            // Ensure the key is long enough for HS512 (>= 64 bytes). If too short, derive a 64-byte key using SHA-512
            if (keyBytes.length < 64) {
                try {
                    MessageDigest md = MessageDigest.getInstance("SHA-512");
                    keyBytes = md.digest(keyBytes);
                } catch (NoSuchAlgorithmException e) {
                    // shouldn't happen; fallback to Keys.secretKeyFor
                    logger.warn("SHA-512 not available to derive JWT key; falling back to generated key");
                    return Keys.secretKeyFor(SignatureAlgorithm.HS512);
                }
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Set<String> roles) {
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
            Object roles = claims.get("roles");
            if (roles instanceof List) return (List<String>) roles;
        } catch (JwtException e) {
            // ignore
        }
        return List.of();
    }

    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
