package com.sudosleep.backend.controller;

import com.sudosleep.backend.config.SecurityConfig;
import com.sudosleep.backend.config.TimeConfig;
import com.sudosleep.backend.dto.AuthResponse;
import com.sudosleep.backend.dto.UserResponse;
import com.sudosleep.backend.entity.AuthProvider;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.exception.GlobalExceptionHandler;
import com.sudosleep.backend.security.JwtService;
import com.sudosleep.backend.service.AuthService;
import com.sudosleep.backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, TimeConfig.class, JwtService.class, GlobalExceptionHandler.class})
class SecurityAndWebWiringTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    @MockitoBean private AuthService authService;
    @MockitoBean private UserService userService;

    @Test
    void protectedEndpointRejectsUnauthenticatedRequestWith401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registerEndpointIsPublicAndReturns201() throws Exception {
        UserResponse userResp = new UserResponse(
                1L, "ana@uni.ro", "Ana", AuthProvider.LOCAL, false, "15,60", Instant.parse("2026-10-01T00:00:00Z"));
        when(authService.register(any())).thenReturn(
                new AuthResponse("access-123", "refresh-123", "Bearer", 3600, userResp, 10L));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Ana","email":"ana@uni.ro","password":"Password123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-123"))
                .andExpect(jsonPath("$.activeSemesterId").value(10));
    }

    @Test
    void protectedEndpointAcceptsValidBearerJwtAndResolvesPrincipal() throws Exception {
        User u = new User("ana@uni.ro", "Ana", "hash", AuthProvider.LOCAL);
        ReflectionTestUtils.setField(u, "id", 42L);
        String token = jwtService.generateAccessToken(u);

        when(userService.get(42L, 42L)).thenReturn(new UserResponse(
                42L, "ana@uni.ro", "Ana", AuthProvider.LOCAL, true, "15,60", Instant.parse("2026-10-01T00:00:00Z")));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.displayName").value("Ana"))
                .andExpect(jsonPath("$.hasCompletedOnboarding").value(true));
    }
}
