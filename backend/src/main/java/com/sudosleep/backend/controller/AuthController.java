package com.sudosleep.backend.controller;

import com.sudosleep.backend.dto.AuthResponse;
import com.sudosleep.backend.dto.FacebookAuthRequest;
import com.sudosleep.backend.dto.ForgotPasswordRequest;
import com.sudosleep.backend.dto.GoogleAuthRequest;
import com.sudosleep.backend.dto.LoginRequest;
import com.sudosleep.backend.dto.MessageResponse;
import com.sudosleep.backend.dto.RefreshTokenRequest;
import com.sudosleep.backend.dto.RegisterRequest;
import com.sudosleep.backend.dto.ResetPasswordRequest;
import com.sudosleep.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
@SecurityRequirements
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new account with email, password and display name")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a fresh access/refresh token pair")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request);
    }

    @PostMapping("/oauth/google")
    @Operation(summary = "Verify a Google ID token and sign in or create/link a user account")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        return service.loginWithGoogle(request);
    }

    @PostMapping("/oauth/facebook")
    @Operation(summary = "Verify a Facebook access token and sign in or create/link a user account")
    public AuthResponse loginWithFacebook(@Valid @RequestBody FacebookAuthRequest request) {
        return service.loginWithFacebook(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a 6-digit password reset code sent to the account's email")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return service.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using the 6-digit code sent via forgot-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return service.resetPassword(request);
    }
}
