package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.IdempotencyKey;
import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.exception.IdempotencyKeyReuseException;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.repository.IdempotencyKeyRepository;
import com.rahul.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

// Claim the key and save the PENDING payment in one short transaction.
// The gateway call happens AFTER this commits, so no connection is held while we wait for the bank.
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private static final Duration KEY_TTL = Duration.ofHours(24);

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional(rollbackFor = Exception.class)
    public PaymentClaim claimAndCreatePending(Long userId, String keyValue, String requestHash,
                                              Long bookingId, BigDecimal amount) {
        int claimed = idempotencyKeyRepository.insertIfAbsent(userId, keyValue, requestHash, LocalDateTime.now().plus(KEY_TTL));
        IdempotencyKey key = idempotencyKeyRepository.findByUserIdAndKeyValue(userId, keyValue).orElseThrow();

        if (claimed == 0) {   // repeat: return the saved payment, do not charge again
            if (!key.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException("Idempotency-Key was already used with a different request");
            }
            Payment existing = paymentRepository.findById(key.getResourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id " + key.getResourceId()));
            return new PaymentClaim(existing, true);
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        Payment saved = paymentRepository.save(payment);

        key.setResourceId(saved.getId());
        return new PaymentClaim(saved, false);
    }
}