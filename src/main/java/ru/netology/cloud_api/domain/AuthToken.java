package ru.netology.cloud_api.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Arrays;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    @Column(name = "token_hash", nullable = false)
    private byte[] tokenHash; // PK: bytea(32)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "ip_hash")
    private byte[] ipHash;

    @Column(name = "user_agent")
    private String userAgent;

    public AuthToken() {
    }

    public AuthToken(byte[] tokenHash, User user, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public byte[] getTokenHash() {
        return tokenHash;
    }

    public User getUser() {
        return user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public byte[] getIpHash() {
        return ipHash;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public void setIpHash(byte[] ipHash) {
        this.ipHash = ipHash;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthToken that)) return false;
        return Arrays.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(tokenHash);
    }
}
