package com.rahul.notificationservice.email;

// Strategy pattern: NotificationService depends on this interface only.
// A real SMTP or provider class can replace the fake one later.
public interface EmailSender {

    // throws an exception if the email could not be sent
    void send(String to, String subject, String body);
}