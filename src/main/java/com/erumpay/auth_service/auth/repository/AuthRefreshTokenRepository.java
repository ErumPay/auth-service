package com.erumpay.auth_service.auth.repository;

import com.erumpay.auth_service.auth.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHashAndIsRevokedFalse(String tokenHash);

    List<AuthRefreshToken> findAllByUser_UserIdAndIsRevokedFalse(Long userId);

    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.isRevoked = true WHERE t.user.userId = :userId AND t.isRevoked = false")
    int revokeAllByUserId(Long userId);

    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.isRevoked = true WHERE t.tokenHash = :tokenHash")
    int revokeByTokenHash(String tokenHash);
}
