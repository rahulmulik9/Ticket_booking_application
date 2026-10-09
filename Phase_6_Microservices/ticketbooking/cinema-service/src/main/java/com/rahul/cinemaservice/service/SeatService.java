package com.rahul.cinemaservice.service;

import com.rahul.cinemaservice.entity.Seat;
import com.rahul.cinemaservice.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final ShowService showService;

    public List<Seat> getSeatsByShow(Long showId) {
        showService.getShowById(showId);   // 404 if the show does not exist
        return seatRepository.findByShowIdOrderById(showId);
    }

    // reserve and release (with the optimistic lock) are added in the next part
}