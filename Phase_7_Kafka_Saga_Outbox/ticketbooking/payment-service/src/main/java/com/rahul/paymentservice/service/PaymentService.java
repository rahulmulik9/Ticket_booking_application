package com.rahul.paymentservice.service;

import com.rahul.paymentservice.dto.CreatePaymentRequest;
import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.gateway.GatewayResult;
import com.rahul.paymentservice.gateway.PaymentGateway;
import com.rahul.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    // Used by the REST endpoint. No events, same behaviour as before.
    public Payment createPayment(CreatePaymentRequest request) {
        Payment pending = createPending(request.getBookingId(), request.getAmount());
        GatewayResult result = paymentGateway.charge(pending.getBookingId(), pending.getAmount());
        return finish(pending, result);
    }

    // No @Transactional on purpose. Each save commits by itself, so the database
    // connection is not held while we wait for the gateway (a real gateway can be slow).
    // The PENDING row is saved first, so a crash during the call still leaves a record.
    public Payment createPending(Long bookingId, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    public Payment finish(Payment pending, GatewayResult result) {
        pending.setStatus(result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        Payment finished = paymentRepository.save(pending);

        log.info("Payment {} for booking {} finished as {}", finished.getId(), finished.getBookingId(), finished.getStatus());
        return finished;
    }

    public Payment getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id " + id));
    }
}