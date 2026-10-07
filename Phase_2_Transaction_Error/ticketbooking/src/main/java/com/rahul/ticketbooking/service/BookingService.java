package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.enums.BookingStatus;
import com.rahul.ticketbooking.repository.BookingRepository;
import com.rahul.ticketbooking.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // no @Transactional here: the transaction starts in the other bean
    public BookingResponse createBooking(BookingRequest request) {
        return bookingTransactionService.createBooking(request);
    }

    @Transactional
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));
        List<Long> seatIds = bookingSeatRepository.findSeatIdsByBookingId(id);
        return BookingResponse.from(booking, seatIds);
    }

    // controller calls this directly, so the proxy works
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking " + id + " is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Seat> seats = bookingSeatRepository.findSeatsByBookingId(id);
        seatService.releaseSeats(seats);

        log.info("Cancelled booking {} and released {} seats", id, seats.size());
        return BookingResponse.from(booking, seats.stream().map(Seat::getId).sorted().toList());
    }
}