package com.rahul.bookingservice.client;

import com.rahul.bookingservice.config.PaymentFeignConfig;
import com.rahul.bookingservice.dto.CreatePaymentRequest;
import com.rahul.bookingservice.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", configuration = PaymentFeignConfig.class)
public interface PaymentClient {

    @PostMapping("/api/v1/payments")
    PaymentResponse createPayment(@RequestBody CreatePaymentRequest request);
}