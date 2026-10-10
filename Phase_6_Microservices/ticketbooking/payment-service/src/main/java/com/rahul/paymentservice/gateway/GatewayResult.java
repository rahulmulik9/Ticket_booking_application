package com.rahul.paymentservice.gateway;

import lombok.AllArgsConstructor;
import lombok.Getter;

// What a gateway answers: approved or declined, with a short reason.
@Getter
@AllArgsConstructor
public class GatewayResult {

    private boolean success;
    private String message;
}