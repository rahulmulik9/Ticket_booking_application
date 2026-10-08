package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // the ownership check is part of the query itself
    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
}