package com.rahul.ticketbooking.repository;

import com.rahul.ticketbooking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowIdOrderById(Long showId);


    /*select s.id, s.show_id, s.seat_number, s.status
    from seats s
    where s.id in (?, ?, ?)
    and s.show_id = ?*/
    List<Seat> findByIdInAndShowId(List<Long> ids, Long showId);

    // select ... for update (Hibernate may print "for no key update", same idea for us).
    // The lock is held until the surrounding transaction commits or rolls back.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Seat> findByIdInAndShowIdOrderById(List<Long> ids, Long showId);
}