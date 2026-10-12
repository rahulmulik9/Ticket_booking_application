package com.rahul.paymentservice.controller;

import com.rahul.paymentservice.dto.CreatePaymentRequest;
import com.rahul.paymentservice.dto.PaymentResponse;
import com.rahul.paymentservice.dto.RefundResponse;
import com.rahul.paymentservice.mapper.PaymentMapper;
import com.rahul.paymentservice.service.PaymentService;
import com.rahul.paymentservice.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payments")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final RefundService refundService;

    @Operation(summary = "Pay for a booking. Send a unique Idempotency-Key header.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Payment recorded, check the status field"),
            @ApiResponse(responseCode = "400", description = "Invalid body or missing Idempotency-Key"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "422", description = "Idempotency-Key reused with a different request")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@AuthenticationPrincipal Long userId,
                                         @RequestHeader("Idempotency-Key") String idempotencyKey,
                                         @Valid @RequestBody CreatePaymentRequest request) {
        return paymentMapper.toResponse(paymentService.createPayment(userId, idempotencyKey, request));
    }

    @Operation(summary = "Get a payment by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found"),
            @ApiResponse(responseCode = "404", description = "Payment not found")
    })
    @GetMapping("/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.getPaymentById(id));
    }


    @Operation(summary = "Refund a successful payment (admin only). Also used to retry a failed refund.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed, check the status field (DONE or FAILED)"),
            @ApiResponse(responseCode = "404", description = "Payment not found"),
            @ApiResponse(responseCode = "409", description = "Payment is not in SUCCESS state")
    })
    @PostMapping("/{id}/refund")
    public RefundResponse refundPayment(@PathVariable Long id) {
        return RefundResponse.from(refundService.refund(id));
    }
}