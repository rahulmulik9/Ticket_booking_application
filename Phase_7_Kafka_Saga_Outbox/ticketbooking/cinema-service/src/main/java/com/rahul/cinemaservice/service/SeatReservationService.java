package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.exception.ResourceNotFoundException;
import com.rahul.cinemaservice.exception.SeatNotAvailableException;
import com.rahul.cinemaservice.kafka.event.SeatBookedEvent;
import com.rahul.cinemaservice.kafka.event.SeatBookingFailedEvent;
import com.rahul.cinemaservice.kafka.event.SeatReservationEvent;
import com.rahul.cinemaservice.kafka.publisher.CinemaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


//Why this new class? : It is a separate bean, so the @Transactional on SeatService.reserveSeats works through its proxy.
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatReservationService {

    private final SeatService seatService;
    private final ShowService showService;
    private final CinemaEventPublisher cinemaEventPublisher;

    // Same rules as the old /internal reserve call. The exceptions that used to become 404 or 409
    // now become a SeatBookingFailedEvent, because nobody is waiting for an HTTP answer.
    public void reserve(SeatReservationEvent event) {
        List<Long> seatIds = event.getSeatIds().stream().distinct().toList();
        try {
            seatService.reserveSeats(event.getShowId(), seatIds);
        } catch (ResourceNotFoundException | SeatNotAvailableException ex) {
            reject(event, ex.getMessage());
            return;
        } catch (OptimisticLockingFailureException ex) {
            reject(event, "Someone else just took one of these seats, please try again");
            return;
        }

        BigDecimal total = showService.getShowById(event.getShowId()).getPrice()
                .multiply(BigDecimal.valueOf(seatIds.size()));
        cinemaEventPublisher.publishSeatBooked(new SeatBookedEvent(event.getBookingId(), event.getShowId(), seatIds, total));
    }

    private void reject(SeatReservationEvent event, String reason) {
        log.info("Seats of booking {} rejected: {}", event.getBookingId(), reason);
        cinemaEventPublisher.publishSeatBookingFailed(new SeatBookingFailedEvent(event.getBookingId(), event.getShowId(), reason));
    }
}