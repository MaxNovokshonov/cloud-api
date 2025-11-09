package ru.netology.cloud_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.cloud_api.domain.AuthToken;
import ru.netology.cloud_api.dto.ErrorResponse;
import ru.netology.cloud_api.repository.AuthTokenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final AuthTokenRepository tokens;
    private final ObjectMapper objectMapper;

    public AuthTokenFilter(AuthTokenRepository tokens, ObjectMapper objectMapper) {
        this.tokens = tokens;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/login".equals(path)
                || "/error".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(req);
        if (token == null || token.isBlank()) {
            unauthorized(res, "Unauthorized: missing auth-token");
            return;
        }
        try {
            byte[] hash = sha256(token);
            Optional<AuthToken> opt = tokens.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash, Instant.now());
            if (opt.isEmpty()) {
                unauthorized(res, "Unauthorized: invalid or expired token");
                return;
            }
            AuthToken at = opt.get();
            at.setLastUsedAt(Instant.now());
            tokens.save(at);

            AbstractAuthenticationToken auth = new AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
                @Override
                public Object getCredentials() {
                    return null;
                }

                @Override
                public Object getPrincipal() {
                    return at.getUser().getId();
                }
            };
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(req, res);
        } catch (Exception e) {
            unauthorized(res, "Unauthorized");
        }
    }

    private static String extractToken(HttpServletRequest request) {
        String v = cleanupBearer(request.getHeader("auth-token"));
        if (hasText(v)) return v;

        v = cleanupBearer(request.getHeader("Authorization"));
        if (hasText(v)) return v;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("auth-token".equals(c.getName())) {
                    v = cleanupBearer(c.getValue());
                    if (hasText(v)) return v;
                }
            }
        }
        return null;
    }

    private static String cleanupBearer(String raw) {
        if (raw == null) return null;
        String val = raw.trim();
        if (val.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            val = val.substring(7).trim();
        }
        return val;
    }

    private void unauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(1002, message)));
    }

    private static byte[] sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(s.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
