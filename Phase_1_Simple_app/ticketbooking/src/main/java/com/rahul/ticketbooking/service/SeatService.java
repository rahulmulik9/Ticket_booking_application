package com.rahul.ticketbooking.service;

import com.rahul.ticketbooking.entity.Seat;
import com.rahul.ticketbooking.enums.SeatStatus;
import com.rahul.ticketbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final ShowService showService;

    public List<Seat> getSeatsByShow(Long showId) {
        showService.getShowById(showId);   // fails if the show does not exist
        return seatRepository.findByShowIdOrderById(showId);
    }

    public List<Seat> getSeatsForBooking(Long showId, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new RuntimeException("Select at least one seat");
        }
        List<Long> distinctIds = seatIds.stream().distinct().toList();
        List<Seat> seats = seatRepository.findByIdInAndShowId(distinctIds, showId);
        if (seats.size() != distinctIds.size()) {
            throw new RuntimeException("One or more seats do not exist for show " + showId);
        }
        return seats;
    }

    public void reserveSeats(List<Seat> seats) {
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new RuntimeException("Seat " + seat.getSeatNumber() + " is already booked");
            }
        }
        seats.forEach(seat -> seat.setStatus(SeatStatus.BOOKED));
        seatRepository.saveAll(seats);
    }

    public void releaseSeats(List<Seat> seats) {
        seats.forEach(seat -> seat.setStatus(SeatStatus.AVAILABLE));
        seatRepository.saveAll(seats);
    }
}