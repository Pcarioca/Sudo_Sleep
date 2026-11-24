package com.sudosleep.backend.security;

import com.sudosleep.backend.entity.AuthProvider;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-that-is-definitely-longer-than-32-bytes-123456";
    private static final Instant T0 = Instant.parse("2026-10-01T10:00:00Z");

    private static User user(Long id, String email, String name) {
        User u = new User(email, name, "hash", AuthProvider.LOCAL);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void generatesAndValidatesAccessAndRefreshTokens() {
        JwtProperties props = new JwtProperties(TEST_SECRET, 3600, 86400);
        JwtService service = new JwtService(props, Clock.fixed(T0, ZoneOffset.UTC));
        User u = user(42L, "ana@uni.ro", "Ana Popescu");

        String access = service.generateAccessToken(u);
        String refresh = service.generateRefreshToken(u);

        AuthenticatedUser accessPrincipal = service.parseAndValidate(access, JwtService.TOKEN_TYPE_ACCESS);
        assertEquals(42L, accessPrincipal.id());
        assertEquals("ana@uni.ro", accessPrincipal.email());

        AuthenticatedUser refreshPrincipal = service.parseAndValidate(refresh, JwtService.TOKEN_TYPE_REFRESH);
        assertEquals(42L, refreshPrincipal.id());
        assertEquals("ana@uni.ro", refreshPrincipal.email());
    }

    @Test
    void rejectsWrongTokenType() {
        JwtProperties props = new JwtProperties(TEST_SECRET, 3600, 86400);
        JwtService service = new JwtService(props, Clock.fixed(T0, ZoneOffset.UTC));
        User u = user(7L, "ion@uni.ro", "Ion");

        String refresh = service.generateRefreshToken(u);
        assertThrows(UnauthorizedException.class,
                () -> service.parseAndValidate(refresh, JwtService.TOKEN_TYPE_ACCESS));

        String access = service.generateAccessToken(u);
        assertThrows(UnauthorizedException.class,
                () -> service.parseAndValidate(access, JwtService.TOKEN_TYPE_REFRESH));
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties props = new JwtProperties(TEST_SECRET, 60, 3600);
        JwtService issuer = new JwtService(props, Clock.fixed(T0, ZoneOffset.UTC));
        JwtService laterValidator = new JwtService(props, Clock.fixed(T0.plusSeconds(61), ZoneOffset.UTC));

        String access = issuer.generateAccessToken(user(1L, "a@b.ro", "A"));
        assertThrows(UnauthorizedException.class,
                () -> laterValidator.parseAndValidate(access, JwtService.TOKEN_TYPE_ACCESS));
    }

    @Test
    void rejectsTamperedOrBlankToken() {
        JwtProperties props = new JwtProperties(TEST_SECRET, 3600, 86400);
        JwtService service = new JwtService(props, Clock.fixed(T0, ZoneOffset.UTC));

        assertThrows(UnauthorizedException.class,
                () -> service.parseAndValidate("not.a.jwt", JwtService.TOKEN_TYPE_ACCESS));
        assertThrows(UnauthorizedException.class,
                () -> service.parseAndValidate("   ", JwtService.TOKEN_TYPE_ACCESS));
    }
}
