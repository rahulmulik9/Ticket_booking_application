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

import java.util.List;

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
    public BookingResponse createBooking(BookingRequest request) {
        log.debug("Booking request: showId={}, userId={}, seatCount={}", request.getShowId(), request.getUserId(), request.getSeatIds().size());
        try {
            return bookingTransactionService.createBooking(request);
        } catch (ConcurrencyFailureException e) {
            // optimistic version clash, lock timeout, or deadlock victim
            log.warn("Seat conflict for showId={}, userId={}: {}", request.getShowId(), request.getUserId(), e.getClass().getSimpleName());
            throw new SeatNotAvailableException("Seat is being booked by someone else, please try again");
        }
    }

    @Transactional
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        List<Long> seatIds = bookingSeatRepository.findSeatIdsByBookingId(id);
        return BookingResponse.from(booking, seatIds);
    }

    // controller calls this directly, so the proxy works
    @Transactional(rollbackFor = Exception.class)
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
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
}