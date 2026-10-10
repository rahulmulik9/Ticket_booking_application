package com.rahul.bookingservice.enums;

public enum BookingStatus {
    CREATED,          // seats held, waiting for payment
    CONFIRMED,        // paid
    CANCELLED,
    PAYMENT_FAILED    // payment declined or unreachable, seats were freed
}