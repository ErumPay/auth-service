package com.erumpay.auth_service.sms.repository;

import com.erumpay.auth_service.sms.entity.AuthSmsVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AuthSmsVerificationRepository extends JpaRepository<AuthSmsVerification, Long> {

    Optional<AuthSmsVerification> findByVerificationIdAndIsVerifiedFalseAndDeletedAtIsNull(Long verificationId);

    Optional<AuthSmsVerification> findByVerificationIdAndIsVerifiedTrueAndDeletedAtIsNull(Long verificationId);

    boolean existsByUser_UserIdAndIsVerifiedTrueAndDeletedAtIsNull(Long userId);

    @Query("SELECT COUNT(s) > 0 FROM AuthSmsVerification s WHERE s.phoneNumberHash = :phoneNumberHash AND s.isVerified = false AND s.deletedAt IS NULL AND s.expiresAt > :now")
    boolean existsValidRequestByPhoneHash(String phoneNumberHash, java.time.LocalDateTime now);

    @Modifying
    @Query("UPDATE AuthSmsVerification s SET s.deletedAt = CURRENT_TIMESTAMP WHERE s.user.userId = :userId AND s.deletedAt IS NULL")
    int softDeleteByUserId(Long userId);
}
