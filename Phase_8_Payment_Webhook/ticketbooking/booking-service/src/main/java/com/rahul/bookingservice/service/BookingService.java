package com.rahul.bookingservice.service;

import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.repository.BookingRepository;
import com.rahul.bookingservice.util.IdempotencyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingTransactionService bookingTransactionService;
    private final BookingRepository bookingRepository;

    public Booking createBooking(Long userId, String userEmail, String idempotencyKey, BookingRequest request) {

        IdempotencyUtil.validateKey(idempotencyKey);

        // drop duplicates and sort, so [2,1] and [1,2] count as the same request
        List<Long> seatIds = new ArrayList<>(new TreeSet<>(request.getSeatIds()));
        String requestHash = IdempotencyUtil.sha256(request.getShowId() + ":" + seatIds);

        Booking booking = bookingTransactionService.savePending(userId, userEmail, idempotencyKey, requestHash, request.getShowId(), seatIds);

        log.info("Booking {} is PENDING for user {} (key {})", booking.getId(), userId, idempotencyKey);
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