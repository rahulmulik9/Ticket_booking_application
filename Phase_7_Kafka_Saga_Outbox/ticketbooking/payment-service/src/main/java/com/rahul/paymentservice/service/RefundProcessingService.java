package com.rahul.paymentservice.service;

import com.rahul.paymentservice.entity.Refund;
import com.rahul.paymentservice.enums.RefundStatus;
import com.rahul.paymentservice.exception.RefundNotAllowedException;
import com.rahul.paymentservice.exception.ResourceNotFoundException;
import com.rahul.paymentservice.kafka.event.RefundCompletedEvent;
import com.rahul.paymentservice.kafka.event.RefundFailedEvent;
import com.rahul.paymentservice.kafka.event.RefundRequestEvent;
import com.rahul.paymentservice.kafka.publisher.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundProcessingService {

    private final RefundService refundService;
    private final PaymentEventPublisher paymentEventPublisher;

    public void process(RefundRequestEvent event) {
        try {
            Refund refund = refundService.refund(event.getPaymentId());

            if (refund.getStatus() == RefundStatus.DONE) {
                paymentEventPublisher.publishRefundCompleted(new RefundCompletedEvent(
                        event.getBookingId(), event.getPaymentId(), refund.getId(), refund.getAmount()));
            } else {
                paymentEventPublisher.publishRefundFailed(new RefundFailedEvent(
                        event.getBookingId(), event.getPaymentId(), refund.getId(), "Refund declined by the gateway"));
            }
        } catch (ResourceNotFoundException | RefundNotAllowedException ex) {
            log.warn("Refund for booking {} not possible: {}", event.getBookingId(), ex.getMessage());
            paymentEventPublisher.publishRefundFailed(new RefundFailedEvent(
                    event.getBookingId(), event.getPaymentId(), null, ex.getMessage()));
        }
    }
}