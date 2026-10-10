package com.rahul.paymentservice.gateway;

import java.math.BigDecimal;

// Strategy pattern: PaymentService depends on this interface only.
// A real provider later is just another class that implements it.
public interface PaymentGateway {

    GatewayResult charge(Long bookingId, BigDecimal amount);
}