package com.erumpay.auth_service.auth.repository;

import com.erumpay.auth_service.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByKakaoOauthId(String kakaoOauthId);

    Optional<AuthUser> findByPhoneNumberHash(String phoneNumberHash);

    boolean existsByKakaoOauthId(String kakaoOauthId);

    boolean existsByPhoneNumberHash(String phoneNumberHash);

    List<AuthUser> findByUserIdIn(List<Long> userIds);
}
