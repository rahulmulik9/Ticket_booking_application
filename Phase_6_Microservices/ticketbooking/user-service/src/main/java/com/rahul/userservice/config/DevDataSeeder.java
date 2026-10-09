package com.rahul.userservice.config;

import com.rahul.userservice.entity.User;
import com.rahul.userservice.enums.Role;
import com.rahul.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Runs only when app.seed.enabled=true. Keep it false outside development.
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seed("Admin", "admin@ticket.com", "Admin@123", Role.ADMIN);
        seed("Organizer", "organizer@ticket.com", "Organizer@123", Role.ORGANIZER);
    }

    private void seed(String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        log.info("Seeded {} account {}", role, email);
    }
}