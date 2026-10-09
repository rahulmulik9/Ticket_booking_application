package com.rahul.userservice.service;

import com.rahul.userservice.dto.AuthResponse;
import com.rahul.userservice.dto.LoginRequest;
import com.rahul.userservice.dto.RegisterRequest;
import com.rahul.userservice.entity.RefreshToken;
import com.rahul.userservice.entity.User;
import com.rahul.userservice.enums.Role;
import com.rahul.userservice.exception.DuplicateResourceException;
import com.rahul.userservice.exception.UnauthorizedException;
import com.rahul.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // Not @Transactional on purpose: if the database rejects a duplicate email,
    // we catch it here. Inside a transaction that catch would not be safe.
    public User register(RegisterRequest request) {
        String email = normalize(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);   // everyone who signs up is a USER, never ORGANIZER or ADMIN

        try {
            User saved = userRepository.saveAndFlush(user);
            log.info("Registered user {}", saved.getId());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // two sign-ups with the same email at the same moment: the unique constraint wins
            throw new DuplicateResourceException("Email is already registered");
        }
    }

    public AuthResponse login(LoginRequest request) {
        // same message for "no such user" and "wrong password"
        User user = userRepository.findByEmail(normalize(request.getEmail()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        log.info("User {} logged in", user.getId());
        return issueTokens(user);
    }

    // Rotation: the old refresh token is revoked and a new one is issued.
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken old = refreshTokenService.validate(rawRefreshToken);
        old.setRevoked(true);
        return issueTokens(old.getUser());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.create(user);
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getAccessTokenSeconds());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}