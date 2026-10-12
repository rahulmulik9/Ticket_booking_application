package com.rahul.paymentservice.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Pretends to be a bank. Approves every amount up to a limit, declines the rest.
// The limit and the refund switch come from config, so you can test the failed paths without changing code.
@Component
@Slf4j
public class FakePaymentGateway implements PaymentGateway {

    private final BigDecimal maxAmount;
    private final boolean failRefunds;

    public FakePaymentGateway(@Value("${payment.gateway.max-amount:10000}") BigDecimal maxAmount,
                              @Value("${payment.gateway.fail-refunds:false}") boolean failRefunds) {
        this.maxAmount = maxAmount;
        this.failRefunds = failRefunds;
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

    @Override
    public GatewayResult refund(Long bookingId, BigDecimal amount) {
        if (failRefunds) {
            log.info("Fake gateway declined the refund of booking {}", bookingId);
            return new GatewayResult(false, "Refund declined by the bank");
        }
        log.info("Fake gateway approved the refund of booking {}", bookingId);
        return new GatewayResult(true, "Refunded");
    }
}