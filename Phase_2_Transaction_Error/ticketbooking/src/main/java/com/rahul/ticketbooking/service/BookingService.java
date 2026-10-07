package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.dto.BookingRequest;
import com.rahul.ticketbooking.dto.BookingResponse;
import com.rahul.ticketbooking.entity.Booking;
import com.rahul.ticketbooking.entity.BookingSeat;
import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.entity.Show;
import com.rahul.ticketbooking.enums.BookingStatus;
import com.rahul.ticketbooking.repository.BookingRepository;
import com.rahul.ticketbooking.repository.BookingSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowService showService;
    private final SeatService seatService;

    public BookingResponse createBooking(BookingRequest request) {
        Show show = showService.getShowById(request.getShowId());
        List<Seat> seats = seatService.getSeatsForBooking(show.getId(), request.getSeatIds());

        seatService.reserveSeats(seats);

        BigDecimal totalAmount = show.getPrice().multiply(BigDecimal.valueOf(seats.size()));

        Booking booking = new Booking();
        booking.setShow(show);
        booking.setUserId(request.getUserId());
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> links = new ArrayList<>();
        for (Seat seat : seats) {
            BookingSeat link = new BookingSeat();
            link.setBooking(savedBooking);
            link.setSeat(seat);
            links.add(link);
        }
        bookingSeatRepository.saveAll(links);

        log.info("Created booking {} for show {} with {} seats", savedBooking.getId(), show.getId(), seats.size());
        return BookingResponse.from(savedBooking, seats.stream().map(Seat::getId).sorted().toList());
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id " + id));
        List<Long> seatIds = bookingSeatRepository.findSeatIdsByBookingId(id);
        return BookingResponse.from(booking, seatIds);
    }

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