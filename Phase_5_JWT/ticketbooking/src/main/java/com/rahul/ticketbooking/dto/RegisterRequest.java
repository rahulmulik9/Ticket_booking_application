package com.rahul.ticketbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email is not valid")
    @Size(max = 150, message = "email must be at most 150 characters")
    private String email;

    // BCrypt only reads the first 72 bytes, so we cap the length there
    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be 8 to 72 characters")
    private String password;
}