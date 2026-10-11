package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Payment;
import com.rahul.paymentservice.entity.Refund;
import com.rahul.paymentservice.enums.PaymentStatus;
import com.rahul.paymentservice.enums.RefundStatus;
import com.rahul.paymentservice.exception.RefundNotAllowedException;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.gateway.GatewayResult;
import com.rahul.paymentservice.gateway.PaymentGateway;
import com.rahul.paymentservice.repository.PaymentRepository;
import com.rahul.paymentservice.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentGateway paymentGateway;

    // Used by the event listener and by the REST endpoint.
    // A FAILED refund leaves the payment as SUCCESS, so it can be tried again.
    public Refund refund(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RefundNotAllowedException("Only a SUCCESS payment can be refunded, this one is " + payment.getStatus());
        }

        Refund refund = new Refund();
        refund.setPaymentId(payment.getId());
        refund.setAmount(payment.getAmount());
        refund.setStatus(RefundStatus.PENDING);
        Refund pending = refundRepository.save(refund);

        GatewayResult result = paymentGateway.refund(payment.getBookingId(), payment.getAmount());

        if (result.isSuccess()) {
            pending.setStatus(RefundStatus.DONE);
            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        } else {
            pending.setStatus(RefundStatus.FAILED);
        }
        Refund finished = refundRepository.save(pending);

        log.info("Refund {} of payment {} finished as {}", finished.getId(), paymentId, finished.getStatus());
        return finished;
    }
}