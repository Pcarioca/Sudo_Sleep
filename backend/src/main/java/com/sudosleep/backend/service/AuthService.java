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
import com.sudosleep.backend.security.AuthenticatedUser;
import com.sudosleep.backend.security.FacebookTokenVerifier;
import com.sudosleep.backend.security.GoogleTokenVerifier;
import com.sudosleep.backend.security.JwtService;
import com.sudosleep.backend.security.SocialUserInfo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuthService {

    static final long PASSWORD_RESET_TTL_MINUTES = 15L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository users;
    private final SemesterRepository semesters;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleVerifier;
    private final FacebookTokenVerifier facebookVerifier;
    private final PasswordResetMailService mailService;
    private final UserMapper userMapper;
    private final Clock clock;

    public AuthService(UserRepository users,
                       SemesterRepository semesters,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleTokenVerifier googleVerifier,
                       FacebookTokenVerifier facebookVerifier,
                       PasswordResetMailService mailService,
                       UserMapper userMapper,
                       Clock clock) {
        this.users = users;
        this.semesters = semesters;
        this.resetTokens = resetTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleVerifier = googleVerifier;
        this.facebookVerifier = facebookVerifier;
        this.mailService = mailService;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        String hash = passwordEncoder.encode(request.password());
        User user = users.save(new User(email, request.displayName().trim(), hash, AuthProvider.LOCAL));
        Semester semester = ensureDefaultSemester(user);
        return issueAuthResponse(user, semester.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = users.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        Semester semester = ensureDefaultSemester(user);
        return issueAuthResponse(user, semester.getId());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        AuthenticatedUser principal = jwtService.parseAndValidate(
                request.refreshToken(), JwtService.TOKEN_TYPE_REFRESH);
        User user = users.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        Semester semester = ensureDefaultSemester(user);
        return issueAuthResponse(user, semester.getId());
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        SocialUserInfo info = googleVerifier.verify(request.idToken());
        String email = normalizeEmail(info.email());

        User user = users.findByGoogleSub(info.providerUserId()).orElse(null);
        if (user == null && info.emailVerified()) {
            user = users.findByEmail(email).orElse(null);
            if (user != null) {
                user.setGoogleSub(info.providerUserId());
                if (user.getAuthProvider() != AuthProvider.GOOGLE) {
                    user.setAuthProvider(AuthProvider.LINKED);
                }
            }
        }
        if (user == null) {
            if (users.existsByEmail(email)) {
                throw new ConflictException(
                        "An account with this email exists, but Google email ownership is not verified");
            }
            user = new User(email, sanitizeDisplayName(info.displayName(), email), null, AuthProvider.GOOGLE);
            user.setGoogleSub(info.providerUserId());
            user = users.save(user);
        }

        Semester semester = ensureDefaultSemester(user);
        return issueAuthResponse(user, semester.getId());
    }

    @Transactional
    public AuthResponse loginWithFacebook(FacebookAuthRequest request) {
        SocialUserInfo info = facebookVerifier.verify(request.accessToken());

        User user = users.findByFacebookId(info.providerUserId()).orElse(null);
        if (user == null) {
            if (info.email() == null || info.email().isBlank()) {
                throw new BadRequestException(
                        "Facebook account did not provide an email address. Grant email permission or register with email.");
            }
            String email = normalizeEmail(info.email());
            user = users.findByEmail(email).orElse(null);
            if (user != null) {
                user.setFacebookId(info.providerUserId());
                if (user.getAuthProvider() != AuthProvider.FACEBOOK) {
                    user.setAuthProvider(AuthProvider.LINKED);
                }
            } else {
                user = new User(email, sanitizeDisplayName(info.displayName(), email), null, AuthProvider.FACEBOOK);
                user.setFacebookId(info.providerUserId());
                user = users.save(user);
            }
        }

        Semester semester = ensureDefaultSemester(user);
        return issueAuthResponse(user, semester.getId());
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        users.findByEmail(email).ifPresent(user -> {
            // Invalidate any previous unused reset codes for this user
            for (PasswordResetToken existing : resetTokens.findByUserIdAndUsedFalse(user.getId())) {
                existing.setUsed(true);
            }
            String code = generateSixDigitCode();
            Instant now = Instant.now(clock);
            PasswordResetToken token = new PasswordResetToken(
                    user,
                    sha256Hex(code),
                    now.plusSeconds(PASSWORD_RESET_TTL_MINUTES * 60),
                    now);
            resetTokens.save(token);
            mailService.sendResetCode(user.getEmail(), user.getDisplayName(), code, PASSWORD_RESET_TTL_MINUTES);
        });
        // Always return generic success message to prevent email enumeration
        return new MessageResponse(
                "If an account exists for that email address, a password reset code has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.email());
        User user = users.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset code"));

        String tokenHash = sha256Hex(request.code().trim());
        PasswordResetToken token = resetTokens
                .findFirstByUserIdAndTokenHashAndUsedFalseOrderByCreatedAtDesc(user.getId(), tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset code"));

        if (token.getExpiresAt().isBefore(Instant.now(clock))) {
            token.setUsed(true);
            throw new BadRequestException("Password reset code has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        if (user.getAuthProvider() == AuthProvider.GOOGLE || user.getAuthProvider() == AuthProvider.FACEBOOK) {
            user.setAuthProvider(AuthProvider.LINKED);
        }
        token.setUsed(true);
        for (PasswordResetToken other : resetTokens.findByUserIdAndUsedFalse(user.getId())) {
            other.setUsed(true);
        }
        return new MessageResponse("Password has been reset successfully. You can now log in.");
    }

    // ---- internals --------------------------------------------------------------------

    private Semester ensureDefaultSemester(User user) {
        List<Semester> existing = semesters.findByUserIdOrderByStartDateDesc(user.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String termName = today.getYear() + " Semester";
        return semesters.save(new Semester(user, termName, monday, SemesterService.DEFAULT_WEEKS_COUNT));
    }

    private AuthResponse issueAuthResponse(User user, Long activeSemesterId) {
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                "Bearer",
                jwtService.accessExpirationSeconds(),
                userMapper.toResponse(user),
                activeSemesterId);
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeDisplayName(String name, String email) {
        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();
            return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : "Sudo Scholar";
    }

    private static String generateSixDigitCode() {
        return String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
