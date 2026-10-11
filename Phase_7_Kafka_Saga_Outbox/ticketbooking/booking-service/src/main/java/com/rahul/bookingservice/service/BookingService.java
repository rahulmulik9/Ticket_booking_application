package com.rahul.bookingservice.service;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.kafka.publisher.BookingEventPublisher;
import com.rahul.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingTransactionService bookingTransactionService;
    private final BookingRepository bookingRepository;
    private final BookingEventPublisher bookingEventPublisher;

    // Save the request and hand the rest to Cinema through Kafka. The user does not wait.
    public Booking createBooking(Long userId, BookingRequest request) {
        List<Long> seatIds = new ArrayList<>(new LinkedHashSet<>(request.getSeatIds()));   // drop duplicates

        Booking booking = bookingTransactionService.savePending(userId, request.getShowId(), seatIds);
        bookingEventPublisher.publishSeatReservation(booking);

        log.info("Booking {} saved as PENDING for user {}", booking.getId(), userId);
        return booking;
    }

    public Booking getBooking(Long userId, Long bookingId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));
    }

    public List<Booking> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // The cancel saga: the booking is CANCELLED at once, then three services react to their own event.
    // Cinema frees the seats, Payment refunds the money, Notification tells the user.
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingTransactionService.cancelBooking(userId, bookingId);

        bookingEventPublisher.publishSeatRelease(booking, "CANCELLED");
        bookingEventPublisher.publishRefundRequest(booking);
        bookingEventPublisher.publishBookingCancelled(booking);

        log.info("Booking {} cancelled by user {}", bookingId, userId);
        return booking;
    }
}