package com.rahul.paymentservice.dto;

import com.rahul.paymentservice.entity.Refund;
import com.rahul.paymentservice.enums.RefundStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RefundResponse {

    private Long id;
    private Long paymentId;
    private BigDecimal amount;
    private RefundStatus status;

    public static RefundResponse from(Refund refund) {
        RefundResponse response = new RefundResponse();
        response.setId(refund.getId());
        response.setPaymentId(refund.getPaymentId());
        response.setAmount(refund.getAmount());
        response.setStatus(refund.getStatus());
        return response;
    }
}