package com.erumpay.auth_service.pin.entity;

import com.erumpay.auth_service.auth.entity.AuthUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_pin")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pin_id")
    private Long pinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AuthUser user;

    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(name = "pin_salt", nullable = false, length = 128)
    private String pinSalt;

    @Column(name = "fail_count", nullable = false)
    @Builder.Default
    private Integer failCount = 0;

    @Column(name = "fail_last_at")
    private LocalDateTime failLastAt;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "active_user_id", insertable = false, updatable = false)
    private Long activeUserId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean isLocked() {
        return this.lockedUntil != null && this.lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean requiresSmsVerification() {
        return this.failCount >= 10;
    }
}
