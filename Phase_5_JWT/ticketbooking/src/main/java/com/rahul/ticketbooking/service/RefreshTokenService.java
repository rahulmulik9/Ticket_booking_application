package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.RefreshToken;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.exception.InvalidCredentialsException;
import com.rahul.ticketbooking.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final String INVALID_MESSAGE = "Invalid or expired refresh token";

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh-token-days}") long refreshTokenDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenDays = refreshTokenDays;
    }

    // returns the raw token. This is the only moment it exists in readable form.
    @Transactional
    public String create(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(rawToken));
        entity.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenDays));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);

        return rawToken;
    }

    // marks the token as used and returns its owner. A token works only once.
    @Transactional
    public User consume(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHashWithUser(hash(rawToken))
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_MESSAGE));

        // atomic: if two requests arrive together, only one gets 1 here
        int updated = refreshTokenRepository.revokeIfActive(token.getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new InvalidCredentialsException(INVALID_MESSAGE);
        }
        return token.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.revokeByTokenHash(hash(rawToken));
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}