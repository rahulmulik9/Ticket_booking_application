package com.rahul.paymentservice.controller;

import com.rahul.paymentservice.dto.PaymentWebhookRequest;
import com.rahul.paymentservice.exception.InvalidWebhookSignatureException;
import com.rahul.paymentservice.service.PaymentWebhookService;
import com.rahul.paymentservice.service.WebhookSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Webhooks")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookSignatureService signatureService;
    private final PaymentWebhookService paymentWebhookService;

    @Operation(summary = "Payment result from the gateway. Called by the gateway, not by users.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accepted (also returned for duplicates and ignored calls)"),
            @ApiResponse(responseCode = "400", description = "Invalid body"),
            @ApiResponse(responseCode = "401", description = "Missing or wrong X-Signature"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @PostMapping("/payment")
    public ResponseEntity<Void> paymentWebhook(
            @RequestHeader(value = WebhookSignatureService.SIGNATURE_HEADER, required = false) String signature,
            @Valid @RequestBody PaymentWebhookRequest request) {

        if (!signatureService.isValid(request.getEventId(), request.getPaymentId(), request.getStatus(), signature)) {
            throw new InvalidWebhookSignatureException("Invalid webhook signature");
        }
        paymentWebhookService.handle(request);
        return ResponseEntity.ok().build();   // 2xx tells the gateway "got it, stop retrying"
    }
}