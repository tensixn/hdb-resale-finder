package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entity: a signed-in session. The JWT carries the session id, so signing out (UC-15) or an
 * inactivity timeout (TBD-5) can end the session on the server even though the token is stateless.
 */
@Entity
@Table(name = "user_session")
public class Session {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected Session() {
    }

    public Session(String id, Long userId, Instant now, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = now;
        this.lastSeenAt = now;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
