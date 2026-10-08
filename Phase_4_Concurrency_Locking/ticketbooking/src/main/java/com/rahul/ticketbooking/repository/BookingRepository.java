package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}