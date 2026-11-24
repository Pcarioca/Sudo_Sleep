package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.UserProfileUpdateRequest;
import com.sudosleep.backend.dto.UserRequest;
import com.sudosleep.backend.dto.UserResponse;
import com.sudosleep.backend.security.AuthenticatedUser;
import com.sudosleep.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user's profile")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.get(currentUser.id(), currentUser.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Update the currently authenticated user's profile (displayName, onboarding, reminder defaults)")
    public UserResponse updateMe(@Valid @RequestBody UserProfileUpdateRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.updateProfile(currentUser.id(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete the currently authenticated user and all their data")
    public void deleteMe(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        service.delete(currentUser.id(), currentUser.id());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user (must match authenticated user)")
    public UserResponse get(@PathVariable("id") Long id,
                            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.get(id, currentUser.id());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user (must match authenticated user)")
    public UserResponse update(@PathVariable("id") Long id,
                               @Valid @RequestBody UserRequest request,
                               @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.update(id, currentUser.id(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user and everything that belongs to them (must match authenticated user)")
    public void delete(@PathVariable("id") Long id,
                       @AuthenticationPrincipal AuthenticatedUser currentUser) {
        service.delete(id, currentUser.id());
    }
}
