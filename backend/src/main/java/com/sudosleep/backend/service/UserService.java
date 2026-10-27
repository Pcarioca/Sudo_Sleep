package com.sudosleep.backend.service;

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

    public UserResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        User user = users.save(new User(email, request.displayName().trim()));
        return mapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = find(id);
        String email = normalizeEmail(request.email());
        if (users.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("Email already registered: " + email);
        }
        user.setEmail(email);
        user.setDisplayName(request.displayName().trim());
        return mapper.toResponse(user);
    }

    /** Deletes the user; semesters, subjects, grades and timetable are removed by the database (ON DELETE CASCADE). */
    @Transactional
    public void delete(Long id) {
        users.delete(find(id));
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
