package com.rahul.paymentservice.gateway;

import lombok.Getter;

// What a gateway answers: approved, declined, or "ask me later" (PENDING).
@Getter
public class GatewayResult {

    private final GatewayStatus status;
    private final String message;

    private GatewayResult(GatewayStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public static GatewayResult success(String message) {
        return new GatewayResult(GatewayStatus.SUCCESS, message);
    }

    public static GatewayResult failed(String message) {
        return new GatewayResult(GatewayStatus.FAILED, message);
    }

    public static GatewayResult pending(String message) {
        return new GatewayResult(GatewayStatus.PENDING, message);
    }

    public boolean isSuccess() {
        return status == GatewayStatus.SUCCESS;
    }

    public boolean isPending() {
        return status == GatewayStatus.PENDING;
    }
}