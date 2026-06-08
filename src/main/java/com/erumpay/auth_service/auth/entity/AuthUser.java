package com.erumpay.auth_service.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "kakao_oauth_id", nullable = false, unique = true, length = 100)
    private String kakaoOauthId;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "phone_number_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)")
    private String phoneNumberHash;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "birth_date")
    private String birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "service_terms_agreed_at")
    private LocalDateTime serviceTermsAgreedAt;

    @Column(name = "privacy_terms_agreed_at")
    private LocalDateTime privacyTermsAgreedAt;

    @Column(name = "marketing_terms_agreed_at")
    private LocalDateTime marketingTermsAgreedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = UserStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum UserStatus {
        PENDING, ACTIVE, SUSPENDED, WITHDRAWN
    }
}
