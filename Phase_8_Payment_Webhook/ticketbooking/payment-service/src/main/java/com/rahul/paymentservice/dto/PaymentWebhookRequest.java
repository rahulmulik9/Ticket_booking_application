package com.rahul.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentWebhookRequest {

    @NotBlank(message = "eventId is required")
    @Size(max = 64, message = "eventId must be at most 64 characters")
    private String eventId;

    @NotNull(message = "paymentId is required")
    private Long paymentId;

    @NotBlank(message = "status is required")
    @Pattern(regexp = "SUCCESS|FAILED|PENDING", message = "status must be SUCCESS, FAILED or PENDING")
    private String status;
}