package com.banking.accountservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    /**
     * Issues a signed JWT for the given account.
     * Subject = accountNumber, extra claim = username.
     * Account-service only ISSUES tokens; validation happens at the gateway.
     */
    public String generateToken(String accountNumber, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(accountNumber)
                .claim("username", username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
