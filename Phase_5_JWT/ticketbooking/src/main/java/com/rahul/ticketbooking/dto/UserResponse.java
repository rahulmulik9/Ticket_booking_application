package com.rahul.ticketbooking.dto;

import com.rahul.ticketbooking.entity.User;
import com.rahul.ticketbooking.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;

    // the password hash is never sent out
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}