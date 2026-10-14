package com.rahul.notificationservice.service;

import com.rahul.notificationservice.email.EmailSender;
import com.rahul.notificationservice.entity.NotificationLog;
import com.rahul.notificationservice.enums.NotificationStatus;
import com.rahul.notificationservice.enums.NotificationType;
import com.rahul.notificationservice.kafka.event.BookingCancelledEvent;
import com.rahul.notificationservice.kafka.event.BookingCompletedEvent;
import com.rahul.notificationservice.kafka.event.BookingFailedEvent;
import com.rahul.notificationservice.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Builds the message, sends it, and records SENT or FAILED.
// Called inside the transaction that IdempotencyService.runOnce starts.
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final EmailSender emailSender;
    private final NotificationLogRepository notificationLogRepository;

    public void bookingConfirmed(BookingCompletedEvent event) {
        send(event.getBookingId(), event.getEmail(), NotificationType.BOOKING_CONFIRMED,
                "Booking " + event.getBookingId() + " confirmed",
                "Your booking " + event.getBookingId() + " is confirmed. Amount paid: "
                        + event.getTotalAmount() + " (payment " + event.getPaymentId() + "). Enjoy the show!");
    }

    public void bookingFailed(BookingFailedEvent event) {
        String why = switch (String.valueOf(event.getReason())) {
            case "PAYMENT_FAILED" -> "the payment did not go through";
            case "SEATS_UNAVAILABLE" -> "the seats were no longer available";
            default -> String.valueOf(event.getReason());
        };
        send(event.getBookingId(), event.getEmail(), NotificationType.BOOKING_FAILED,
                "Booking " + event.getBookingId() + " failed",
                "Sorry, your booking " + event.getBookingId() + " could not be completed because " + why + ".");
    }

    public void bookingCancelled(BookingCancelledEvent event) {
        send(event.getBookingId(), event.getEmail(), NotificationType.BOOKING_CANCELLED,
                "Booking " + event.getBookingId() + " cancelled",
                "Your booking " + event.getBookingId() + " is cancelled. Your refund is on its way.");
    }

    private void send(Long bookingId, String email, NotificationType type, String subject, String body) {
        NotificationLog entry = new NotificationLog();
        entry.setBookingId(bookingId);
        entry.setType(type);
        entry.setRecipient(email);
        entry.setSubject(subject);
        entry.setMessage(body);

        try {
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("No email address on the event");
            }
            emailSender.send(email, subject, body);
            entry.setStatus(NotificationStatus.SENT);
        } catch (Exception e) {
            // An email problem must never block booking events, so we record it and move on.
            log.error("Could not send {} email for booking {}: {}", type, bookingId, e.getMessage());
            entry.setStatus(NotificationStatus.FAILED);
            entry.setFailureReason(e.getMessage());
        }
        notificationLogRepository.save(entry);
    }
}