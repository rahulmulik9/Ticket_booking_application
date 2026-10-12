package com.rahul.userservice.service;

import com.rahul.userservice.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Only User service signs tokens. The other services get the same secret and only verify.
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTokenMinutes;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-token-minutes}") long accessTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenMinutes * 60_000);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))   // the user id, read by every service
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())    // USER, ORGANIZER or ADMIN
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long getAccessTokenSeconds() {
        return accessTokenMinutes * 60;
    }
}