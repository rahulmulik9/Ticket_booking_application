package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.RegisterRequest;
import com.rahul.ticketbooking.dto.UserResponse;
import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.enums.Role;
import com.rahul.ticketbooking.exception.EmailAlreadyExistsException;
import com.rahul.ticketbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest request) {
        // same person must not get two accounts because of upper/lower case
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);   // self-registration is always USER

        User saved = userRepository.save(user);
        log.info("Registered user with id {}", saved.getId());   // never log the password or the hash
        return UserResponse.from(saved);
    }
}