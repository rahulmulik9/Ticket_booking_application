package com.rahul.bookingservice.service;

import com.rahul.bookingservice.client.CinemaClient;
import com.rahul.bookingservice.client.PaymentClient;
import com.rahul.bookingservice.dto.BookingRequest;
import com.rahul.bookingservice.dto.CreatePaymentRequest;
import com.rahul.bookingservice.dto.InternalShowResponse;
import com.rahul.bookingservice.dto.PaymentResponse;
import com.rahul.bookingservice.dto.SeatActionRequest;
import com.rahul.bookingservice.entity.Booking;
import com.rahul.bookingservice.exception.PaymentFailedException;
import com.rahul.bookingservice.exception.PaymentUnavailableException;
import com.rahul.bookingservice.exception.ResourceNotFoundException;
import com.rahul.bookingservice.repository.BookingRepository;
import feign.FeignException;
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

    private static final String PAYMENT_SUCCESS = "SUCCESS";

    private final CinemaClient cinemaClient;
    private final PaymentClient paymentClient;
    private final BookingTransactionService bookingTransactionService;
    private final BookingRepository bookingRepository;

    // No @Transactional here. Three services and two databases are involved,
    // so one transaction cannot cover them. Phase 7 replaces this chain with events and a saga.
    public Booking createBooking(Long userId, BookingRequest request) {
        List<Long> seatIds = new ArrayList<>(new LinkedHashSet<>(request.getSeatIds()));   // drop duplicates

        // 1. does the show exist, and what does a seat cost?
        InternalShowResponse show = cinemaClient.getShow(request.getShowId());

        // 2. Cinema checks the seats, locks them and marks them BOOKED (404 or 409 if not possible)
        cinemaClient.reserveSeats(show.getId(), new SeatActionRequest(seatIds));

        // 3. save our side as CREATED. Payment needs the booking id, so this comes before paying.
        BigDecimal total = show.getPrice().multiply(BigDecimal.valueOf(seatIds.size()));
        Booking booking;
        try {
            booking = bookingTransactionService.saveBooking(userId, show.getId(), seatIds, total);
        } catch (RuntimeException ex) {
            releaseQuietly(show.getId(), seatIds);
            throw ex;
        }

        // 4. pay. A decline or an outage ends here: the booking becomes PAYMENT_FAILED and the seats are freed.
        PaymentResponse payment = pay(booking.getId(), total, show.getId(), seatIds);

        // 5. paid, so confirm
        try {
            Booking confirmed = bookingTransactionService.markConfirmed(booking.getId(), payment.getId());
            log.info("Booking {} confirmed for user {} with payment {}", confirmed.getId(), userId, payment.getId());
            return confirmed;
        } catch (RuntimeException ex) {
            log.error("Payment {} succeeded but booking {} could not be confirmed", payment.getId(), booking.getId(), ex);
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
    // The refund of the payment is added in Phase 7.
    public Booking cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingTransactionService.cancelBooking(userId, bookingId);
        releaseQuietly(booking.getShowId(), booking.seatIds());
        log.info("Booking {} cancelled by user {}", bookingId, userId);
        return booking;
    }

    private PaymentResponse pay(Long bookingId, BigDecimal total, Long showId, List<Long> seatIds) {
        PaymentResponse payment;
        try {
            payment = paymentClient.createPayment(new CreatePaymentRequest(bookingId, total));
        } catch (FeignException ex) {
            // We cannot tell whether Payment charged before it failed. Known gap, closed in Phase 7.
            log.error("Payment call failed for booking {}", bookingId, ex);
            failBooking(bookingId, showId, seatIds);
            throw new PaymentUnavailableException("Payment service is unavailable, try again shortly");
        }

        if (!PAYMENT_SUCCESS.equals(payment.getStatus())) {
            log.info("Payment {} for booking {} was declined", payment.getId(), bookingId);
            failBooking(bookingId, showId, seatIds);
            throw new PaymentFailedException("Payment was declined");
        }
        return payment;
    }

    // Database first, seats after, same order as cancel.
    private void failBooking(Long bookingId, Long showId, List<Long> seatIds) {
        try {
            bookingTransactionService.markPaymentFailed(bookingId);
        } catch (RuntimeException ex) {
            log.error("Could not mark booking {} as PAYMENT_FAILED", bookingId, ex);
        }
        releaseQuietly(showId, seatIds);
    }

    private void releaseQuietly(Long showId, List<Long> seatIds) {
        try {
            cinemaClient.releaseSeats(showId, new SeatActionRequest(seatIds));
        } catch (RuntimeException ex) {
            log.error("Could not release seats {} of show {}. They stay BOOKED until fixed by hand.", seatIds, showId, ex);
        }
    }
}