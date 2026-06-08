package com.erumpay.auth_service.device.repository;

import com.erumpay.auth_service.device.entity.AuthDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthDeviceTokenRepository extends JpaRepository<AuthDeviceToken, Long> {

    List<AuthDeviceToken> findByUser_UserIdAndIsActiveTrue(Long userId);

    Optional<AuthDeviceToken> findByUser_UserIdAndDeviceId(Long userId, String deviceId);

    Optional<AuthDeviceToken> findByFcmToken(String fcmToken);

    @Modifying
    @Query("UPDATE AuthDeviceToken t SET t.isActive = false WHERE t.user.userId = :userId AND t.deviceId = :deviceId")
    int deactivateByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Query("UPDATE AuthDeviceToken t SET t.isActive = false WHERE t.user.userId = :userId")
    int deactivateAllByUserId(Long userId);
}
