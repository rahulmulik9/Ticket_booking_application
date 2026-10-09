package com.rahul.paymentservice.enums;

public enum PaymentStatus {
    PENDING,   // saved, gateway not answered yet
    SUCCESS,
    FAILED
    // REFUNDED is added in Phase 7
}