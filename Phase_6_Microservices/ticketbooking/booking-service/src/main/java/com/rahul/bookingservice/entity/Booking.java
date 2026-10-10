package com.rahul.bookingservice.entity;

import com.rahul.bookingservice.enums.BookingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Phase 6: plain ids instead of @ManyToOne Show and User. Those tables are in other services.
    @Column(nullable = false)
    private Long showId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // a real relation: bookings and booking_seats share one database
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookingSeat> bookingSeats = new ArrayList<>();

    @Column
    private Long paymentId;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


    public void addSeat(Long seatId) {
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setBooking(this);
        bookingSeat.setSeatId(seatId);
        this.bookingSeats.add(bookingSeat);
    }

    public List<Long> seatIds() {
        return bookingSeats.stream().map(BookingSeat::getSeatId).toList();
    }
}