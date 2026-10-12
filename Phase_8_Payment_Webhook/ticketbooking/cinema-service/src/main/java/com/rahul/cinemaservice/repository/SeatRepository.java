package com.rahul.cinemaservice.repository;

import com.rahul.cinemaservice.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // all seats of a show, for the seat map
    List<Seat> findByShowIdOrderById(Long showId);

    // seats picked for a booking. Ordered by id on purpose: every request reads
    // its seats in the same order, so two bookings cannot lock each other in a circle (deadlock).
    List<Seat> findByIdInAndShowIdOrderById(List<Long> ids, Long showId);
}