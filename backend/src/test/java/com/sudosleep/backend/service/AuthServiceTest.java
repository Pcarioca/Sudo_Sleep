package com.sudosleep.backend.service;

import com.sudosleep.backend.dto.AuthResponse;
import com.sudosleep.backend.dto.FacebookAuthRequest;
import com.sudosleep.backend.dto.ForgotPasswordRequest;
import com.sudosleep.backend.dto.GoogleAuthRequest;
import com.sudosleep.backend.dto.LoginRequest;
import com.sudosleep.backend.dto.MessageResponse;
import com.sudosleep.backend.dto.RefreshTokenRequest;
import com.sudosleep.backend.dto.RegisterRequest;
import com.sudosleep.backend.dto.ResetPasswordRequest;
import com.sudosleep.backend.entity.AuthProvider;
import com.sudosleep.backend.entity.PasswordResetToken;
import com.sudosleep.backend.entity.Semester;
import com.sudosleep.backend.entity.User;
import com.sudosleep.backend.exception.BadRequestException;
import com.sudosleep.backend.exception.ConflictException;
import com.sudosleep.backend.exception.UnauthorizedException;
import com.sudosleep.backend.mapper.UserMapper;
import com.sudosleep.backend.repository.PasswordResetTokenRepository;
import com.sudosleep.backend.repository.SemesterRepository;
import com.sudosleep.backend.repository.UserRepository;
import com.sudosleep.backend.security.FacebookTokenVerifier;
import com.sudosleep.backend.security.GoogleTokenVerifier;
import com.sudosleep.backend.security.JwtProperties;
import com.sudosleep.backend.security.JwtService;
import com.sudosleep.backend.security.SocialUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-07T09:00:00Z"); // Wednesday

    @Mock private UserRepository users;
    @Mock private SemesterRepository semesters;
    @Mock private PasswordResetTokenRepository resetTokens;
    @Mock private GoogleTokenVerifier googleVerifier;
    @Mock private FacebookTokenVerifier facebookVerifier;
    @Mock private PasswordResetMailService mailService;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(
                new JwtProperties("unit-test-secret-with-at-least-32-characters-0000", 3600, 86400),
                clock);
        authService = new AuthService(
                users,
                semesters,
                resetTokens,
                passwordEncoder,
                jwtService,
                googleVerifier,
                facebookVerifier,
                mailService,
                new UserMapper(),
                clock);
    }

    @Test
    void registerHashesPasswordCreatesMondaySemesterAndIssuesTokens() {
        when(users.existsByEmail("ana@uni.ro")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 10L);
            return u;
        });
        when(semesters.findByUserIdOrderByStartDateDesc(10L)).thenReturn(List.of());
        when(semesters.save(any(Semester.class))).thenAnswer(inv -> {
            Semester s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 100L);
            return s;
        });

        AuthResponse response = authService.register(
                new RegisterRequest("Ana Popescu", "  Ana@Uni.ro ", "supersecret"));

        assertEquals("Bearer", response.tokenType());
        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertEquals(10L, response.user().id());
        assertEquals("ana@uni.ro", response.user().email());
        assertEquals(AuthProvider.LOCAL, response.user().authProvider());
        assertEquals(100L, response.activeSemesterId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(users).save(userCaptor.capture());
        assertTrue(passwordEncoder.matches("supersecret", userCaptor.getValue().getPasswordHash()));

        ArgumentCaptor<Semester> semCaptor = ArgumentCaptor.forClass(Semester.class);
        verify(semesters).save(semCaptor.capture());
        assertEquals(DayOfWeek.MONDAY, semCaptor.getValue().getStartDate().getDayOfWeek());
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("ana@uni.ro")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> authService.register(new RegisterRequest("Ana", "ana@uni.ro", "password123")));
    }

    @Test
    void loginRejectsWrongPasswordAndSocialOnlyAccounts() {
        User local = new User("ana@uni.ro", "Ana", passwordEncoder.encode("correctpass"), AuthProvider.LOCAL);
        ReflectionTestUtils.setField(local, "id", 1L);
        when(users.findByEmail("ana@uni.ro")).thenReturn(Optional.of(local));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("ana@uni.ro", "wrongpass")));

        User googleOnly = new User("g@uni.ro", "G", null, AuthProvider.GOOGLE);
        ReflectionTestUtils.setField(googleOnly, "id", 2L);
        when(users.findByEmail("g@uni.ro")).thenReturn(Optional.of(googleOnly));

        assertThrows(UnauthorizedException.class,
                () -> authService.login(new LoginRequest("g@uni.ro", "anything")));
    }

    @Test
    void refreshRejectsAccessTokenAndIssuesPairForValidRefreshToken() {
        User u = new User("ana@uni.ro", "Ana", "hash", AuthProvider.LOCAL);
        ReflectionTestUtils.setField(u, "id", 5L);
        Semester sem = new Semester(u, "2026", NOW.atZone(ZoneOffset.UTC).toLocalDate().with(DayOfWeek.MONDAY), 14);
        ReflectionTestUtils.setField(sem, "id", 55L);

        String accessToken = jwtService.generateAccessToken(u);
        assertThrows(UnauthorizedException.class,
                () -> authService.refresh(new RefreshTokenRequest(accessToken)));

        String refreshToken = jwtService.generateRefreshToken(u);
        when(users.findById(5L)).thenReturn(Optional.of(u));
        when(semesters.findByUserIdOrderByStartDateDesc(5L)).thenReturn(List.of(sem));

        AuthResponse refreshed = authService.refresh(new RefreshTokenRequest(refreshToken));
        assertEquals(5L, refreshed.user().id());
        assertEquals(55L, refreshed.activeSemesterId());
    }

    @Test
    void loginWithGoogleLinksExistingLocalAccountWhenEmailVerified() {
        User existing = new User("ana@uni.ro", "Ana", "hash", AuthProvider.LOCAL);
        ReflectionTestUtils.setField(existing, "id", 11L);
        Semester sem = new Semester(existing, "Fall", NOW.atZone(ZoneOffset.UTC).toLocalDate().with(DayOfWeek.MONDAY), 14);
        ReflectionTestUtils.setField(sem, "id", 21L);

        when(googleVerifier.verify("google-id-token"))
                .thenReturn(new SocialUserInfo("google-sub-123", "ana@uni.ro", true, "Ana G"));
        when(users.findByGoogleSub("google-sub-123")).thenReturn(Optional.empty());
        when(users.findByEmail("ana@uni.ro")).thenReturn(Optional.of(existing));
        when(semesters.findByUserIdOrderByStartDateDesc(11L)).thenReturn(List.of(sem));

        AuthResponse response = authService.loginWithGoogle(new GoogleAuthRequest("google-id-token"));

        assertEquals("google-sub-123", existing.getGoogleSub());
        assertEquals(AuthProvider.LINKED, existing.getAuthProvider());
        assertEquals(AuthProvider.LINKED, response.user().authProvider());
        assertEquals(21L, response.activeSemesterId());
    }

    @Test
    void loginWithFacebookRejectsWhenEmailMissingForNewAccount() {
        when(facebookVerifier.verify("fb-token"))
                .thenReturn(new SocialUserInfo("fb-999", null, false, "No Email User"));
        when(users.findByFacebookId("fb-999")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> authService.loginWithFacebook(new FacebookAuthRequest("fb-token")));
    }

    @Test
    void forgotAndResetPasswordFlowUpdatesPasswordAndInvalidatesToken() {
        User u = new User("ana@uni.ro", "Ana", passwordEncoder.encode("oldpass123"), AuthProvider.LOCAL);
        ReflectionTestUtils.setField(u, "id", 9L);

        when(users.findByEmail("ana@uni.ro")).thenReturn(Optional.of(u));
        when(resetTokens.findByUserIdAndUsedFalse(9L)).thenReturn(List.of());

        MessageResponse forgotResp = authService.forgotPassword(new ForgotPasswordRequest("ana@uni.ro"));
        assertNotNull(forgotResp.message());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendResetCode(eq("ana@uni.ro"), eq("Ana"), codeCaptor.capture(), anyLong());
        String plainCode = codeCaptor.getValue();
        assertEquals(6, plainCode.length());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokens).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertEquals(AuthService.sha256Hex(plainCode), savedToken.getTokenHash());

        when(resetTokens.findFirstByUserIdAndTokenHashAndUsedFalseOrderByCreatedAtDesc(
                9L, AuthService.sha256Hex(plainCode)))
                .thenReturn(Optional.of(savedToken));

        MessageResponse resetResp = authService.resetPassword(
                new ResetPasswordRequest("ana@uni.ro", plainCode, "newSecretPass99"));
        assertNotNull(resetResp.message());
        assertTrue(savedToken.isUsed());
        assertTrue(passwordEncoder.matches("newSecretPass99", u.getPasswordHash()));
    }

    @Test
    void forgotPasswordForUnknownEmailStillReturnsGenericSuccessWithoutSendingMail() {
        when(users.findByEmail("ghost@uni.ro")).thenReturn(Optional.empty());

        MessageResponse resp = authService.forgotPassword(new ForgotPasswordRequest("ghost@uni.ro"));
        assertNotNull(resp.message());
        verify(mailService, never()).sendResetCode(any(), any(), any(), anyLong());
    }
}
