package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowIdOrderById(Long showId);

    List<Seat> findByIdInAndShowId(List<Long> ids, Long showId);
}