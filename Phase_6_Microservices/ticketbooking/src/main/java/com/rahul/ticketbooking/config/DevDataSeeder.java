package com.rahul.ticketbooking.config;

import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.enums.Role;
import com.rahul.ticketbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Local development only. @Profile("dev") means this bean does not even exist in prod.
// A default admin with a known password in prod would be a serious security hole.
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private static final String DEV_PASSWORD = "Secret123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedUser("Olivia Organizer", "organizer@example.com", Role.ORGANIZER);
        seedUser("Adam Admin", "admin@example.com", Role.ADMIN);
    }

    private void seedUser(String name, String email, Role role) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(DEV_PASSWORD));
            user.setRole(role);
            userRepository.save(user);
            log.info("Seeded dev user {} with role {}", email, role);
        } else if (user.getRole() != role) {
            // the user was registered earlier as a normal USER (for example by Postman), so fix the role
            user.setRole(role);
            userRepository.save(user);
            log.info("Updated dev user {} to role {}", email, role);
        }
    }
}