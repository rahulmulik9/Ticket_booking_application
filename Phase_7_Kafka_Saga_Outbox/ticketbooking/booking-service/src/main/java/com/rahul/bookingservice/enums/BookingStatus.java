package com.rahul.bookingservice.enums;

public enum BookingStatus {
    PENDING,          // request accepted, waiting for Cinema to reserve the seats
    CREATED,          // seats held, waiting for payment
    CONFIRMED,        // paid
    CANCELLED,
    PAYMENT_FAILED,   // payment declined or unreachable, seats were freed
    REJECTED          // seats were not available
}