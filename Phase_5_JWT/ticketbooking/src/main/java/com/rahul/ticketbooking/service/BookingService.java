package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.enums.BookingStatus;
import com.rahul.ticketbooking.exception.InvalidBookingStateException;
import com.rahul.ticketbooking.exception.ResourceNotFoundException;
import com.rahul.ticketbooking.exception.SeatNotAvailableException;
import com.rahul.ticketbooking.repository.BookingRepository;
import com.rahul.ticketbooking.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatService seatService;
    private final BookingTransactionService bookingTransactionService;

    // no @Transactional here: the transaction starts in the other bean,
    // so a commit-time lock failure comes out here, after the rollback is done.
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        log.debug("Booking request: showId={}, userId={}, seatCount={}", request.getShowId(), userId, request.getSeatIds().size());
        try {
            return bookingTransactionService.createBooking(userId, request);
        } catch (ConcurrencyFailureException e) {
            log.warn("Seat conflict for showId={}, userId={}: {}", request.getShowId(), userId, e.getClass().getSimpleName());
            throw new SeatNotAvailableException("Seat is being booked by someone else, please try again");
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id, Long userId) {
        Booking booking = bookingRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        List<Long> seatIds = bookingSeatRepository.findSeatIdsByBookingId(id);
        return BookingResponse.from(booking, seatIds);
    }


    @Transactional(rollbackFor = Exception.class)
    public BookingResponse cancelBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingStateException("Booking " + id + " is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Seat> seats = bookingSeatRepository.findSeatsByBookingId(id);
        seatService.releaseSeats(seats);

        log.info("Cancelled booking {} and released {} seats", id, seats.size());
        return BookingResponse.from(booking, seats.stream().map(Seat::getId).sorted().toList());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> bookingIds = bookings.stream().map(Booking::getId).toList();
        Map<Long, List<Long>> seatIdsByBooking = new HashMap<>();
        for (Object[] row : bookingSeatRepository.findBookingSeatPairs(bookingIds)) {
            seatIdsByBooking.computeIfAbsent((Long) row[0], key -> new ArrayList<>()).add((Long) row[1]);
        }

        return bookings.stream()
                .map(b -> BookingResponse.from(b, seatIdsByBooking.getOrDefault(b.getId(), List.of())))
                .toList();
    }
}