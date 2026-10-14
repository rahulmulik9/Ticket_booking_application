package com.rahul.paymentservice.gateway;

public enum GatewayStatus {
    SUCCESS,
    FAILED,
    PENDING   // the gateway accepted the request, the answer comes later by webhook
}