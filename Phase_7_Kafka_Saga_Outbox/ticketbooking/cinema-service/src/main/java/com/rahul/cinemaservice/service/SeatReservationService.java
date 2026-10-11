package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.exception.ResourceNotFoundException;
import com.rahul.cinemaservice.exception.SeatNotAvailableException;
import com.rahul.cinemaservice.kafka.event.SeatBookedEvent;
import com.rahul.cinemaservice.kafka.event.SeatBookingFailedEvent;
import com.rahul.cinemaservice.kafka.event.SeatReservationEvent;
import com.rahul.cinemaservice.kafka.publisher.CinemaEventRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatReservationService {

    private final SeatService seatService;
    private final ShowService showService;
    private final CinemaEventRecorder cinemaEventRecorder;
    private final IdempotencyService idempotencyService;
    private final TransactionTemplate transactionTemplate;

    public void reserve(SeatReservationEvent event) {
        if (idempotencyService.isProcessed(event.getEventId())) {
            log.info("SeatReservationEvent {} already processed, skipped", event.getEventId());
            return;
        }

        List<Long> seatIds = event.getSeatIds().stream().distinct().toList();
        try {
            // one transaction: the claim, the seats and the SeatBookedEvent row commit together
            transactionTemplate.executeWithoutResult(status -> {
                if (!idempotencyService.claim(event.getEventId())) {
                    return;   // another thread got there first
                }
                seatService.reserveSeats(event.getShowId(), seatIds);
                BigDecimal total = showService.getShowById(event.getShowId()).getPrice()
                        .multiply(BigDecimal.valueOf(seatIds.size()));
                cinemaEventRecorder.recordSeatBooked(
                        new SeatBookedEvent(event.getBookingId(), event.getShowId(), seatIds, total));
            });
        } catch (ResourceNotFoundException | SeatNotAvailableException ex) {
            reject(event, ex.getMessage());
        } catch (OptimisticLockingFailureException ex) {
            reject(event, "Someone else just took one of these seats, please try again");
        }
    }

    // The failed attempt rolled back, claim included. A new transaction claims the event and saves the rejection.
    private void reject(SeatReservationEvent event, String reason) {
        log.info("Seats of booking {} rejected: {}", event.getBookingId(), reason);
        transactionTemplate.executeWithoutResult(status -> {
            if (!idempotencyService.claim(event.getEventId())) {
                return;
            }
            cinemaEventRecorder.recordSeatBookingFailed(
                    new SeatBookingFailedEvent(event.getBookingId(), event.getShowId(), reason));
        });
    }
}