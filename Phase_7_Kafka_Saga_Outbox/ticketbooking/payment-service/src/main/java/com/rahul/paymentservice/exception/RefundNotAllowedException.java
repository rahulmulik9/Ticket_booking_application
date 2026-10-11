package com.rahul.paymentservice.exception;

public class RefundNotAllowedException extends RuntimeException {

    public RefundNotAllowedException(String message) {
        super(message);
    }
}