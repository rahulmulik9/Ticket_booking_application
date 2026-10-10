package com.rahul.bookingservice.service;

import com.rahul.bookingservice.client.CinemaClient;
import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.dto.SeatActionRequest;
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

    private final CinemaClient cinemaClient;   // only used by cancel until Step 5
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

    // Unchanged for now. The refund and the event-based release come in Step 5.
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingTransactionService.cancelBooking(userId, bookingId);
        releaseQuietly(booking.getShowId(), booking.seatIds());
        log.info("Booking {} cancelled by user {}", bookingId, userId);
        return booking;
    }

    private void releaseQuietly(Long showId, List<Long> seatIds) {
        try {
            cinemaClient.releaseSeats(showId, new SeatActionRequest(seatIds));
        } catch (RuntimeException ex) {
            log.error("Could not release seats {} of show {}. They stay BOOKED until fixed by hand.", seatIds, showId, ex);
        }
    }
}