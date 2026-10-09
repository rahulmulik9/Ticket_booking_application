package com.rahul.bookingservice.service;

import com.rahul.bookingservice.client.CinemaClient;
import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.dto.InternalShowResponse;
import com.rahul.bookingservice.dto.SeatActionRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final CinemaClient cinemaClient;
    private final BookingTransactionService bookingTransactionService;
    private final BookingRepository bookingRepository;

    // No @Transactional here. There are two databases involved (Cinema's and ours),
    // so one transaction cannot cover both. Phase 7 replaces the catch block with a saga.
    public Booking createBooking(Long userId, BookingRequest request) {
        List<Long> seatIds = new ArrayList<>(new LinkedHashSet<>(request.getSeatIds()));   // drop duplicates

        // 1. does the show exist, and what does a seat cost?
        InternalShowResponse show = cinemaClient.getShow(request.getShowId());

        // 2. Cinema checks the seats, locks them and marks them BOOKED (404 or 409 if not possible)
        cinemaClient.reserveSeats(show.getId(), new SeatActionRequest(seatIds));

        // 3. save our side. If it fails, give the seats back.
        BigDecimal total = show.getPrice().multiply(BigDecimal.valueOf(seatIds.size()));
        try {
            Booking booking = bookingTransactionService.saveBooking(userId, show.getId(), seatIds, total);
            log.info("Booking {} created for user {}", booking.getId(), userId);
            return booking;
        } catch (RuntimeException ex) {
            releaseQuietly(show.getId(), seatIds);
            throw ex;
        }
    }

    public Booking getBooking(Long userId, Long bookingId) {
        return bookingRepository.findByIdAndUserId(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));
    }

    public List<Booking> getMyBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // Database first, seats after. If the release call fails, the seats stay BOOKED
    // (safe, but lost sales) instead of being freed for a booking that is still CONFIRMED.
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