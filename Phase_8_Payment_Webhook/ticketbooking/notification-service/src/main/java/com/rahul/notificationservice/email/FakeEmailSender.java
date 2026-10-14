package com.rahul.notificationservice.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Does not send anything. It writes the email to the log.
// Set notification.email.simulate-failure=true to see the FAILED path.
@Component
@Slf4j
public class FakeEmailSender implements EmailSender {

    private final boolean simulateFailure;

    public FakeEmailSender(@Value("${notification.email.simulate-failure:false}") boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public void send(String to, String subject, String body) {
        if (simulateFailure) {
            throw new IllegalStateException("Simulated email failure");
        }
        log.info("EMAIL to {} | subject: {} | body: {}", to, subject, body);
    }
}