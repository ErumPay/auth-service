package com.erumpay.auth_service.auth.controller;

import com.erumpay.auth_service.auth.JwtTokenProvider;
import com.erumpay.auth_service.auth.entity.AuthRefreshToken;
import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthRefreshTokenRepository;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.util.AesEncryptionUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevTokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthUserRepository authUserRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AesEncryptionUtil aesEncryptionUtil;

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<Map<String, String>> createUser(@RequestBody(required = false) DevUserCreateRequest request) {
        DevUserCreateRequest body = request != null ? request : new DevUserCreateRequest();
        String kakaoOauthId = body.getKakaoOauthId() != null && !body.getKakaoOauthId().isBlank()
                ? body.getKakaoOauthId()
                : "dev-user-" + System.currentTimeMillis();

        return authUserRepository.findByKakaoOauthId(kakaoOauthId)
                .map(this::toUserResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(toUserResponse(createDevUser(body, kakaoOauthId))));
    }

    @GetMapping("/token/{userId}")
    @Transactional
    public ResponseEntity<Map<String, String>> issueToken(@PathVariable Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getStatus().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        saveRefreshToken(user, refreshToken);

        return ResponseEntity.ok(Map.of(
                "userId", user.getUserId().toString(),
                "status", user.getStatus().name(),
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }

    private AuthUser createDevUser(DevUserCreateRequest request, String kakaoOauthId) {
        String phoneNumber = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()
                ? request.getPhoneNumber()
                : defaultPhoneNumber();
        String phoneNumberHash = aesEncryptionUtil.hashWithFixedSalt(phoneNumber);

        if (authUserRepository.existsByPhoneNumberHash(phoneNumberHash)) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 가입된 전화번호 (phone_number_hash 중복)");
        }

        LocalDateTime now = LocalDateTime.now();
        AuthUser user = AuthUser.builder()
                .kakaoOauthId(kakaoOauthId)
                .phoneNumber(aesEncryptionUtil.encrypt(phoneNumber))
                .phoneNumberHash(phoneNumberHash)
                .name(request.getName() != null && !request.getName().isBlank() ? request.getName() : "Dev User")
                .birthDate(request.getBirthDate() != null && !request.getBirthDate().isBlank()
                        ? aesEncryptionUtil.encrypt(request.getBirthDate())
                        : null)
                .status(parseStatus(request.getStatus()))
                .serviceTermsAgreedAt(now)
                .privacyTermsAgreedAt(now)
                .marketingTermsAgreedAt(Boolean.TRUE.equals(request.getMarketingTermsAgreed()) ? now : null)
                .build();

        return authUserRepository.save(user);
    }

    private void saveRefreshToken(AuthUser user, String rawToken) {
        refreshTokenRepository.revokeAllByUserId(user.getUserId());
        AuthRefreshToken refreshToken = AuthRefreshToken.builder()
                .user(user)
                .tokenHash(jwtTokenProvider.hashToken(rawToken))
                .deviceInfo("dev-token")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private Map<String, String> toUserResponse(AuthUser user) {
        return Map.of(
                "userId", user.getUserId().toString(),
                "kakaoOauthId", user.getKakaoOauthId(),
                "status", user.getStatus().name()
        );
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return UserStatus.PENDING;
        }

        try {
            return UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자 상태");
        }
    }

    private String defaultPhoneNumber() {
        long suffix = System.currentTimeMillis() % 100_000_000L;
        return String.format("010%08d", suffix);
    }

    @Getter
    @Setter
    public static class DevUserCreateRequest {
        private String kakaoOauthId;
        private String phoneNumber;
        private String name;
        private String birthDate;
        private String status;
        private Boolean marketingTermsAgreed;
    }
}
