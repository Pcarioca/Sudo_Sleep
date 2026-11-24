package com.sudosleep.backend.security;

import com.sudosleep.backend.entity.AuthProvider;
import com.sudosleep.backend.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
        jwtService = new JwtService(
                new JwtProperties("filter-test-secret-that-is-over-32-bytes-long-9999", 3600, 86400),
                clock);
        filter = new JwtAuthenticationFilter(jwtService);
        user = new User("ana@uni.ro", "Ana", "hash", AuthProvider.LOCAL);
        ReflectionTestUtils.setField(user, "id", 77L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsPrincipalForValidBearerAccessToken() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/users/me");
        req.addHeader("Authorization", "Bearer " + jwtService.generateAccessToken(user));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        AuthenticatedUser principal = assertInstanceOf(AuthenticatedUser.class, auth.getPrincipal());
        assertEquals(77L, principal.id());
        assertEquals("ana@uni.ro", principal.email());
    }

    @Test
    void ignoresRefreshTokenOnApiRequests() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/users/me");
        req.addHeader("Authorization", "Bearer " + jwtService.generateRefreshToken(user));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void leavesContextEmptyWhenHeaderMissingOrMalformed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/users/me");
        req.addHeader("Authorization", "Bearer not-a-valid-token");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        MockHttpServletRequest noHeader = new MockHttpServletRequest("GET", "/api/users/me");
        filter.doFilter(noHeader, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
