package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;

// The result of claiming a key: the payment, and whether it already existed.
@Getter
@AllArgsConstructor
public class PaymentClaim {

    private final Payment payment;
    private final boolean replayed;
}