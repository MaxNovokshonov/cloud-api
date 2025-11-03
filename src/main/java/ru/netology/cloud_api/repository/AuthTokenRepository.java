package ru.netology.cloud_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.netology.cloud_api.domain.AuthToken;

import java.time.Instant;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, byte[]> {
    Optional<AuthToken> findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(byte[] hash, Instant now);
}
