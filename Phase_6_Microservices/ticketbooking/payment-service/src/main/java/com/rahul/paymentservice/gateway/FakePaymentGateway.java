package com.rahul.paymentservice.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Pretends to be a bank. Approves every amount up to a limit, declines the rest.
// The limit comes from config, so you can test the FAILED path without changing code.
@Component
@Slf4j
public class FakePaymentGateway implements PaymentGateway{

    private final BigDecimal maxAmount;

    public FakePaymentGateway(@Value("${payment.gateway.max-amount:10000}") BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    @Override
    public GatewayResult charge(Long bookingId, BigDecimal amount) {
        if (amount.compareTo(maxAmount) > 0) {
            log.info("Fake gateway declined booking {}: amount is above the limit", bookingId);
            return new GatewayResult(false, "Amount is above the allowed limit");
        }
        log.info("Fake gateway approved booking {}", bookingId);
        return new GatewayResult(true, "Approved");
    }
}