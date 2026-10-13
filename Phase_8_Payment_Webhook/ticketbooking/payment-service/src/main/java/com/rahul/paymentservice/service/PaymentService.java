package com.rahul.paymentservice.service;

import com.rahul.paymentservice.dto.CreatePaymentRequest;
import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.gateway.GatewayResult;
import com.rahul.paymentservice.gateway.PaymentGateway;
import com.rahul.paymentservice.repository.PaymentRepository;
import com.rahul.paymentservice.util.IdempotencyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


// REST entry point. Called by PaymentController (POST /api/v1/payments).
// Duplicate protection: Idempotency-Key header. Output: the payment in the HTTP response.
// Also holds createPending() and finish(), which PaymentProcessingService reuses.

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentTransactionService paymentTransactionService;

    // Used by the REST endpoint.
    public Payment createPayment(Long userId, String idempotencyKey, CreatePaymentRequest request) {
        IdempotencyUtil.validateKey(idempotencyKey);
        String requestHash = IdempotencyUtil.sha256(request.getBookingId() + ":" + request.getAmount().stripTrailingZeros().toPlainString());

        PaymentClaim claim = paymentTransactionService.claimAndCreatePending(userId, idempotencyKey, requestHash, request.getBookingId(), request.getAmount());

        if (claim.isReplayed()) {
            log.info("Repeated request for key {}, returning payment {}", idempotencyKey, claim.getPayment().getId());
            return claim.getPayment();
        }

        GatewayResult result = paymentGateway.charge(claim.getPayment().getBookingId(), claim.getPayment().getAmount());
        return finish(claim.getPayment(), result);
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