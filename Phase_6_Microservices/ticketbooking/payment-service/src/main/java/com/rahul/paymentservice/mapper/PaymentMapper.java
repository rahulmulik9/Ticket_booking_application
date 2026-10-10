package com.rahul.paymentservice.mapper;

import com.rahul.paymentservice.dto.PaymentResponse;
import com.rahul.paymentservice.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setBookingId(payment.getBookingId());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        return response;
    }
}