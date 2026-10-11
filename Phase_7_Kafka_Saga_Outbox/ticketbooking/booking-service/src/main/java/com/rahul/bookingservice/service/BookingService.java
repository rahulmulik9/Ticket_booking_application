package com.rahul.bookingservice.service;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
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

    // Save the request and its outbox event in one transaction. The user does not wait for Cinema.
    public Booking createBooking(Long userId, BookingRequest request) {
        List<Long> seatIds = new ArrayList<>(new LinkedHashSet<>(request.getSeatIds()));   // drop duplicates

        Booking booking = bookingTransactionService.savePending(userId, request.getShowId(), seatIds);
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

    // The booking is CANCELLED and the three cancel events are saved in one transaction.
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingTransactionService.cancelBooking(userId, bookingId);
        log.info("Booking {} cancelled by user {}", bookingId, userId);
        return booking;
    }
}