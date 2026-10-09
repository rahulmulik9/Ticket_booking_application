package com.rahul.paymentservice.dto;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private PaymentStatus status;

    public static PaymentResponse from(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setBookingId(payment.getBookingId());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        return response;
    }
}