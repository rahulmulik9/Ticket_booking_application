package com.rahul.bookingservice.service;

import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.enums.BookingStatus;
import com.rahul.bookingservice.exception.BookingStateException;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.kafka.publisher.BookingEventRecorder;
import com.rahul.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// Every method here changes the booking AND writes its outbox event in one transaction.
// Either both are saved or neither is, so an event can no longer be lost after a save.
// The status guards make a repeated message harmless: it finds another status and returns false.
@Service
@RequiredArgsConstructor
public class BookingTransactionService {

    private final BookingRepository bookingRepository;
    private final BookingEventRecorder bookingEventRecorder;

    @Transactional(rollbackFor = Exception.class)
    public Booking savePending(Long userId, Long showId, List<Long> seatIds) {
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setShowId(showId);
        booking.setStatus(BookingStatus.PENDING);   // no seats held and no total yet
        seatIds.forEach(booking::addSeat);

        Booking saved = bookingRepository.save(booking);
        bookingEventRecorder.recordSeatReservation(saved);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markSeatsBooked(Long bookingId, BigDecimal totalAmount) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            return false;
        }
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.CREATED);
        bookingEventRecorder.recordPaymentRequest(bookingId, totalAmount);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markRejected(Long bookingId, String reason) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING) {
            return false;
        }
        booking.setStatus(BookingStatus.REJECTED);
        bookingEventRecorder.recordBookingFailed(booking, reason);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markConfirmed(Long bookingId, Long paymentId) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.CREATED) {
            return false;
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        bookingEventRecorder.recordBookingCompleted(booking);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaymentFailed(Long bookingId, String reason) {
        Booking booking = findById(bookingId);
        if (booking.getStatus() != BookingStatus.CREATED) {
            return false;
        }
        booking.setStatus(BookingStatus.PAYMENT_FAILED);
        bookingEventRecorder.recordSeatRelease(booking, "PAYMENT_FAILED");   // the undo step
        bookingEventRecorder.recordBookingFailed(booking, reason);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findForUpdate(bookingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));

        // Only a CONFIRMED booking still owns its seats and its payment.
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingStateException("Only a confirmed booking can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Hibernate.initialize(booking.getBookingSeats());   // the controller reads the seats after the session closes

        bookingEventRecorder.recordSeatRelease(booking, "CANCELLED");
        bookingEventRecorder.recordRefundRequest(booking);
        bookingEventRecorder.recordBookingCancelled(booking);
        return booking;
    }

    private Booking findById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + bookingId));
    }
}