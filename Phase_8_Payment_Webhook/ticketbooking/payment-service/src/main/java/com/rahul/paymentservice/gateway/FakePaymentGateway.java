package com.rahul.paymentservice.gateway;

import com.rahul.paymentservice.service.WebhookSignatureService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Pretends to be a bank. Approves every amount up to a limit, declines the rest.
// In async mode it answers PENDING, then calls our webhook after a delay, like a real gateway.
@Component
@Slf4j
public class FakePaymentGateway implements PaymentGateway {

    private final BigDecimal maxAmount;
    private final boolean failRefunds;
    private final boolean asyncEnabled;
    private final long webhookDelayMs;
    private final boolean sendDuplicateWebhook;
    private final String webhookUrl;
    private final WebhookSignatureService signatureService;

    private final RestClient restClient = RestClient.create();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public FakePaymentGateway(@Value("${payment.gateway.max-amount:10000}") BigDecimal maxAmount,
                              @Value("${payment.gateway.fail-refunds:false}") boolean failRefunds,
                              @Value("${payment.gateway.async-enabled:true}") boolean asyncEnabled,
                              @Value("${payment.gateway.webhook-delay-ms:2000}") long webhookDelayMs,
                              @Value("${payment.gateway.send-duplicate-webhook:false}") boolean sendDuplicateWebhook,
                              @Value("${payment.gateway.webhook-url:http://localhost:${server.port:8080}/api/v1/webhooks/payment}") String webhookUrl,
                              WebhookSignatureService signatureService) {
        this.maxAmount = maxAmount;
        this.failRefunds = failRefunds;
        this.asyncEnabled = asyncEnabled;
        this.webhookDelayMs = webhookDelayMs;
        this.sendDuplicateWebhook = sendDuplicateWebhook;
        this.webhookUrl = webhookUrl;
        this.signatureService = signatureService;
    }

    @Override
    public GatewayResult charge(Long paymentId, Long bookingId, BigDecimal amount) {
        boolean approved = amount.compareTo(maxAmount) <= 0;

        if (!asyncEnabled) {   // the old behaviour: answer right away
            log.info("Fake gateway {} booking {}", approved ? "approved" : "declined", bookingId);
            return approved ? GatewayResult.success("Approved") : GatewayResult.failed("Amount is above the allowed limit");
        }

        log.info("Fake gateway accepted payment {}, the result will come by webhook in {} ms", paymentId, webhookDelayMs);
        scheduler.schedule(() -> sendWebhook(paymentId, approved), webhookDelayMs, TimeUnit.MILLISECONDS);
        return GatewayResult.pending("Processing");
    }

    @Override
    public GatewayResult refund(Long bookingId, BigDecimal amount) {
        if (failRefunds) {
            log.info("Fake gateway declined the refund of booking {}", bookingId);
            return GatewayResult.failed("Refund declined by the bank");
        }
        log.info("Fake gateway approved the refund of booking {}", bookingId);
        return GatewayResult.success("Refunded");
    }

    private void sendWebhook(Long paymentId, boolean approved) {
        String eventId = UUID.randomUUID().toString();
        String status = approved ? "SUCCESS" : "FAILED";
        String signature = signatureService.sign(eventId, paymentId, status);
        // built by hand: three simple fields, no JSON library needed for a fake
        String body = "{\"eventId\":\"" + eventId + "\",\"paymentId\":" + paymentId + ",\"status\":\"" + status + "\"}";

        post(body, signature);
        if (sendDuplicateWebhook) {   // real gateways retry, so the same event can arrive twice
            post(body, signature);
        }
    }

    private void post(String body, String signature) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(WebhookSignatureService.SIGNATURE_HEADER, signature)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Fake gateway delivered the webhook");
        } catch (Exception e) {
            log.error("Fake gateway could not deliver the webhook: {}", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdown();
    }
}