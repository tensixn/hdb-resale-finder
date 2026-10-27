package com.abcde.hdbresale.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Entity: a flat saved by a signed-in Home Buyer (UC-08). One bookmark per user and flat. */
@Entity
@Table(name = "bookmark")
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "flat_id")
    private Long flatId;
    @Column(name = "created_at")
    private Instant createdAt;

    protected Bookmark() {
    }

    public Bookmark(Long userId, Long flatId, Instant createdAt) {
        this.userId = userId;
        this.flatId = flatId;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getFlatId() { return flatId; }
    public Instant getCreatedAt() { return createdAt; }
}
