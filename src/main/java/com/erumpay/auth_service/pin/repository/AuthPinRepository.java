package com.erumpay.auth_service.pin.repository;

import com.erumpay.auth_service.pin.entity.AuthPin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthPinRepository extends JpaRepository<AuthPin, Long> {

    Optional<AuthPin> findByUser_UserIdAndDeletedAtIsNull(Long userId);

    boolean existsByUser_UserIdAndDeletedAtIsNull(Long userId);
}
