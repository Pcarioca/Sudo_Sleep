package com.sudosleep.backend.mapper;

import com.sudosleep.backend.dto.UserResponse;
import com.sudosleep.backend.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
