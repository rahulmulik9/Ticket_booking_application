package com.rahul.ticketbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;        // always "Bearer"
    private long expiresInSeconds;   // lifetime of the access token
}