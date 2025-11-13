package ru.netology.cloud_api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.netology.cloud_api.domain.AuthToken;
import ru.netology.cloud_api.domain.User;
import ru.netology.cloud_api.exception.BadCredentials400Exception;
import ru.netology.cloud_api.exception.Unauthorized401Exception;
import ru.netology.cloud_api.repository.AuthTokenRepository;
import ru.netology.cloud_api.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private AuthTokenRepository tokenRepository;
    private PasswordEncoder encoder;
    private AuthService authService;

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(AuthTokenRepository.class);
        encoder = mock(PasswordEncoder.class);
        authService = new AuthService(userRepository, tokenRepository, encoder);

        user = new User();
        user.setId(userId);
        user.setUsername("user1");
        user.setPasswordHash("$2a$hash");
    }

    @Test
    void login_success_returnsToken_andSaves() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(encoder.matches("pass", "$2a$hash")).thenReturn(true);
        when(tokenRepository.save(any(AuthToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String token = authService.login("user1", "pass");

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertDoesNotThrow(() -> Base64.getUrlDecoder().decode(token));

        verify(userRepository).findByUsername("user1");
        verify(encoder).matches("pass", "$2a$hash");
        verify(tokenRepository).save(any(AuthToken.class));
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(encoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThrows(BadCredentials400Exception.class, () -> authService.login("user1", "bad"));
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void login_userNotFound_throwsBadCredentials() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(BadCredentials400Exception.class, () -> authService.login("ghost", "any"));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void logout_validToken_marksRevoked() {
        String raw = "validForTest";
        byte[] hash = sha256(raw);
        Instant now = Instant.now();
        AuthToken existing = new AuthToken(hash, user, now.minus(1, ChronoUnit.MINUTES), now.plus(1, ChronoUnit.HOURS));

        when(tokenRepository.findById(hash)).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any(AuthToken.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.logout(raw);

        verify(tokenRepository).save(argThat(t -> t.getRevokedAt() != null));
    }

    @Test
    void logout_missingToken_throwsUnauthorized() {
        assertThrows(Unauthorized401Exception.class, () -> authService.logout(null));
        assertThrows(Unauthorized401Exception.class, () -> authService.logout(""));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void logout_expiredToken_throwsUnauthorized() {
        String raw = "expired";
        byte[] hash = sha256(raw);
        Instant now = Instant.now();
        AuthToken expired = new AuthToken(hash, user, now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.HOURS));

        when(tokenRepository.findById(hash)).thenReturn(Optional.of(expired));

        assertThrows(Unauthorized401Exception.class, () -> authService.logout(raw));
        verify(tokenRepository, never()).save(any());
    }

    private static byte[] sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
