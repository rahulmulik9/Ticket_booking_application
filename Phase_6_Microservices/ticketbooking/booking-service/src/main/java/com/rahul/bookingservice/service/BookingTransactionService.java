package com.rahul.bookingservice.service;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.enums.BookingStatus;
import com.rahul.bookingservice.exception.BookingStateException;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Only the database work lives here, in its own bean. BookingService calls it from outside,
// so the @Transactional proxy always works (the self-invocation lesson from Phase 2).
// The Cinema calls stay outside the transaction, so a database connection is never held while waiting on the network.
@Service
@RequiredArgsConstructor
public class BookingTransactionService {

    private final BookingRepository bookingRepository;

    @Transactional(rollbackFor = Exception.class)
    public Booking saveBooking(Long userId, Long showId, List<Long> seatIds, BigDecimal totalAmount) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setShowId(showId);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        seatIds.forEach(booking::addSeat);

        return bookingRepository.save(booking);   // booking_seats are saved by the cascade
    }

    @Transactional(rollbackFor = Exception.class)
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findForUpdate(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingStateException("Booking is already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);   // saved by dirty checking when the transaction commits
        Hibernate.initialize(booking.getBookingSeats());   // load the seats now, the session closes after this method
        return booking;
    }
}