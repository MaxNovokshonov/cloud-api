package ru.netology.cloud_api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.netology.cloud_api.domain.AuthToken;
import ru.netology.cloud_api.dto.ErrorResponse;
import ru.netology.cloud_api.repository.AuthTokenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    private final AuthTokenRepository tokens;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher matcher = new AntPathMatcher();

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
        String token = req.getHeader("auth-token");
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

    private void unauthorized(HttpServletResponse res, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(1002, message)));
    }

    private static byte[] sha256(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(s.getBytes(StandardCharsets.UTF_8));
    }
}

