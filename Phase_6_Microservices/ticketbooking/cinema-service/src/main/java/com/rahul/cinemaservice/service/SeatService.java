package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.enums.SeatStatus;
import com.rahul.cinemaservice.exception.ResourceNotFoundException;
import com.rahul.cinemaservice.exception.SeatNotAvailableException;
import com.rahul.cinemaservice.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;
    private final ShowService showService;

    public List<Seat> getSeatsByShow(Long showId) {
        showService.getShowById(showId);   // 404 if the show does not exist
        return seatRepository.findByShowIdOrderById(showId);
    }

    // All or nothing: either every seat becomes BOOKED, or none does.
    // Two requests can both read a seat as AVAILABLE. Both try to save it, but Hibernate adds
    // "AND version = ?" to the UPDATE, so only the first one succeeds and the other gets an
    // OptimisticLockingFailureException, which GlobalExceptionHandler answers with 409.
    @Transactional(rollbackFor = Exception.class)
    public void reserveSeats(Long showId, List<Long> seatIds) {
        showService.getShowById(showId);

        List<Long> ids = seatIds.stream().distinct().toList();
        List<Seat> seats = seatRepository.findByIdInAndShowIdOrderById(ids, showId);

        // a seat that is missing, or that belongs to another show, is not in the result
        if (seats.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more seats do not exist for show " + showId);
        }

        for (Seat seat : seats) {
            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new SeatNotAvailableException("Seat " + seat.getSeatNumber() + " is already booked");
            }
        }

        seats.forEach(seat -> seat.setStatus(SeatStatus.BOOKED));
        seatRepository.saveAllAndFlush(seats);   // flush now, so a version clash is thrown here and not at commit
        log.info("Reserved seats {} of show {}", ids, showId);
    }

    // Safe to call twice: seats that are already AVAILABLE are skipped.
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeats(Long showId, List<Long> seatIds) {
        showService.getShowById(showId);

        List<Long> ids = seatIds.stream().distinct().toList();
        List<Seat> toRelease = seatRepository.findByIdInAndShowIdOrderById(ids, showId).stream()
                .filter(seat -> seat.getStatus() == SeatStatus.BOOKED)
                .toList();

        toRelease.forEach(seat -> seat.setStatus(SeatStatus.AVAILABLE));
        seatRepository.saveAllAndFlush(toRelease);
        log.info("Released {} of {} seats of show {}", toRelease.size(), ids.size(), showId);
    }
}