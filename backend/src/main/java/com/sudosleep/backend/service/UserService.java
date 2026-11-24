package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.UserProfileUpdateRequest;
import com.sudosleep.backend.dto.UserRequest;
import com.sudosleep.backend.dto.UserResponse;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.exception.ConflictException;
import com.sudosleep.backend.exception.ResourceNotFoundException;
import com.sudosleep.backend.mapper.UserMapper;
import com.sudosleep.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;
    private final UserMapper mapper;

    public UserService(UserRepository users, UserMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    public UserResponse get(Long id, Long currentUserId) {
        OwnershipGuard.requireOwner(id, currentUserId);
        return mapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse updateProfile(Long currentUserId, UserProfileUpdateRequest request) {
        User user = find(currentUserId);
        user.setDisplayName(request.displayName().trim());
        if (request.email() != null && !request.email().isBlank()) {
            String email = normalizeEmail(request.email());
            if (users.existsByEmailAndIdNot(email, currentUserId)) {
                throw new ConflictException("Email already registered: " + email);
            }
            user.setEmail(email);
        }
        if (request.hasCompletedOnboarding() != null) {
            user.setHasCompletedOnboarding(request.hasCompletedOnboarding());
        }
        if (request.defaultReminderOffsetsCsv() != null) {
            user.setDefaultReminderOffsetsCsv(request.defaultReminderOffsetsCsv().trim());
        }
        return mapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, Long currentUserId, UserRequest request) {
        OwnershipGuard.requireOwner(id, currentUserId);
        User user = find(id);
        String email = normalizeEmail(request.email());
        if (users.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email already registered: " + email);
        }
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        return mapper.toResponse(user);
    }

    /** Deletes the user; semesters, subjects, grades, events and timetable are removed by the database (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long id, Long currentUserId) {
        OwnershipGuard.requireOwner(id, currentUserId);
        users.delete(find(id));
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
