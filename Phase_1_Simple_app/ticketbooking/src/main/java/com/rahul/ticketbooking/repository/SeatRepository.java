package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
}