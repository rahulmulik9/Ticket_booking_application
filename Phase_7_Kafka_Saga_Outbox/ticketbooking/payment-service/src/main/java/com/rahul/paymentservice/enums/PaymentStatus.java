package com.rahul.paymentservice.enums;

public enum PaymentStatus {
    PENDING,    // saved, gateway not answered yet
    SUCCESS,
    FAILED,
    REFUNDED    // the money went back after a cancel
}