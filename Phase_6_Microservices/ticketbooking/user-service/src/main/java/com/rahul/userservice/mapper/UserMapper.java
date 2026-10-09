package com.rahul.userservice.mapper;

import com.rahul.userservice.dto.UserResponse;
import com.rahul.userservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // the password hash is never copied here
    public UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        return response;
    }
}