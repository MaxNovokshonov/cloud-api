package ru.netology.cloud_api.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.netology.cloud_api.domain.AuthToken;
import ru.netology.cloud_api.domain.User;
import ru.netology.cloud_api.exception.BadCredentials400Exception;
import ru.netology.cloud_api.repository.AuthTokenRepository;
import ru.netology.cloud_api.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    private static final long TOKEN_TTL_HOURS = 24;

    public AuthService(UserRepository users, AuthTokenRepository tokens, PasswordEncoder encoder) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    public String login(String login, String password) {
        User user = users.findByUsername(login)
                .orElseThrow(() -> new BadCredentials400Exception("Bad credentials"));
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentials400Exception("Bad credentials");
        }
        String rawToken = generateToken();
        byte[] hash = sha256(rawToken);
        Instant now = Instant.now();

        AuthToken at = new AuthToken(hash, user, now, now.plus(TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        at.setLastUsedAt(now);
        tokens.save(at);

        return rawToken;
    }

    private String generateToken() {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
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
