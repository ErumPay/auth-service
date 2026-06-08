package com.erumpay.auth_service.auth.service;

import com.erumpay.auth_service.auth.JwtTokenProvider;
import com.erumpay.auth_service.auth.dto.*;
import com.erumpay.auth_service.auth.entity.AuthRefreshToken;
import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthRefreshTokenRepository;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.util.AesEncryptionUtil;
import com.erumpay.auth_service.device.repository.AuthDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthDeviceTokenRepository deviceTokenRepository;
    private final KakaoOAuthService kakaoOAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final StringRedisTemplate redisTemplate;

    private static final String RT_CACHE_PREFIX = "rt:";
    private static final Duration RT_CACHE_TTL = Duration.ofDays(30);

    @Transactional
    public KakaoOAuthResponse kakaoOAuthLogin(KakaoOAuthRequest request) {
        // 필수 약관 동의 확인
        if (!Boolean.TRUE.equals(request.getServiceTermsAgreed())
                || !Boolean.TRUE.equals(request.getPrivacyTermsAgreed())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "필수 약관(서비스이용약관, 개인정보처리방침) 미동의");
        }

        // 카카오 인가 코드로 토큰 발급 → 사용자 정보 조회
        String kakaoAccessToken = kakaoOAuthService.getAccessToken(request.getAuthorizationCode(), request.getRedirectUri());
        KakaoUserInfo userInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);

        // 기존 회원 확인
        Optional<AuthUser> existingUser = authUserRepository.findByKakaoOauthId(userInfo.getKakaoOauthId());

        if (existingUser.isPresent()) {
            return handleExistingUser(existingUser.get());
        } else {
            return handleNewUser(userInfo, request);
        }
    }

    private KakaoOAuthResponse handleExistingUser(AuthUser user) {
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 가입된 카카오 계정 (kakao_oauth_id 중복)");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getStatus().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
        saveRefreshToken(user, refreshToken);

        return KakaoOAuthResponse.builder()
                .isNewUser(false)
                .userId(user.getUserId())
                .name(user.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(user.getStatus().name())
                .build();
    }

    private KakaoOAuthResponse handleNewUser(KakaoUserInfo userInfo, KakaoOAuthRequest request) {
        // 중복 가입 체크
        String phoneNumberHash = null;
        if (userInfo.getPhoneNumber() != null) {
            phoneNumberHash = aesEncryptionUtil.hashWithFixedSalt(userInfo.getPhoneNumber());
            if (authUserRepository.existsByPhoneNumberHash(phoneNumberHash)) {
                throw new AuthException(HttpStatus.CONFLICT, "이미 가입된 전화번호 (phone_number_hash 중복)");
            }
        }
        if (authUserRepository.existsByKakaoOauthId(userInfo.getKakaoOauthId())) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 가입된 카카오 계정 (kakao_oauth_id 중복)");
        }

        LocalDateTime now = LocalDateTime.now();

        AuthUser newUser = AuthUser.builder()
                .kakaoOauthId(userInfo.getKakaoOauthId())
                .phoneNumber(userInfo.getPhoneNumber() != null ? aesEncryptionUtil.encrypt(userInfo.getPhoneNumber()) : null)
                .phoneNumberHash(phoneNumberHash != null ? phoneNumberHash : aesEncryptionUtil.hashWithFixedSalt(""))
                .name(userInfo.getName())
                .birthDate(userInfo.getBirthDate() != null ? aesEncryptionUtil.encrypt(userInfo.getBirthDate()) : null)
                .status(UserStatus.PENDING)
                .serviceTermsAgreedAt(now)
                .privacyTermsAgreedAt(now)
                .marketingTermsAgreedAt(Boolean.TRUE.equals(request.getMarketingTermsAgreed()) ? now : null)
                .build();

        authUserRepository.save(newUser);

        String accessToken = jwtTokenProvider.createAccessToken(newUser.getUserId(), newUser.getStatus().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(newUser.getUserId());
        saveRefreshToken(newUser, refreshToken);

        return KakaoOAuthResponse.builder()
                .isNewUser(true)
                .userId(newUser.getUserId())
                .name(newUser.getName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(newUser.getStatus().name())
                .build();
    }

    @Transactional
    public TokenRefreshResponse refreshToken(String bearerToken) {
        String token = extractToken(bearerToken);

        if (!jwtTokenProvider.validateToken(token) || !"REFRESH".equals(jwtTokenProvider.getTokenType(token))) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token");
        }

        String tokenHash = jwtTokenProvider.hashToken(token);

        // Redis 캐시에서 먼저 조회 → 없으면 DB 조회
        String cachedUserId = redisTemplate.opsForValue().get(RT_CACHE_PREFIX + tokenHash);
        AuthUser user;
        AuthRefreshToken storedToken = null;

        if (cachedUserId != null) {
            // 캐시 hit — revoke 시 캐시도 삭제하므로 캐시에 있으면 유효한 토큰
            user = authUserRepository.findById(Long.parseLong(cachedUserId))
                    .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없음"));
        } else {
            // 캐시 miss — DB 조회 후 캐시 저장
            storedToken = refreshTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash)
                    .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "폐기된 Refresh Token (is_revoked = TRUE)"));
            user = storedToken.getUser();
            cacheRefreshToken(tokenHash, user.getUserId());
        }

        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.PENDING) {
            throw new AuthException(HttpStatus.FORBIDDEN, "사용자 상태가 ACTIVE가 아님");
        }

        // 새 Access Token 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getStatus().name());

        // Refresh Token Rotation 체크
        boolean rotated = jwtTokenProvider.needsRotation(token);
        String newRefreshToken = token;

        if (rotated) {
            if (storedToken != null) {
                storedToken.setIsRevoked(true);
            } else {
                refreshTokenRepository.revokeByTokenHash(tokenHash);
            }
            evictRefreshTokenCache(tokenHash);
            newRefreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());
            saveRefreshToken(user, newRefreshToken);
        }

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .refreshTokenRotated(rotated)
                .build();
    }

    @Transactional
    public void logout(Long userId, String deviceId) {
        // Redis 캐시 삭제
        evictAllRefreshTokenCache(userId);

        if (deviceId != null && !deviceId.isBlank()) {
            refreshTokenRepository.revokeAllByUserId(userId);
            deviceTokenRepository.deactivateByUserIdAndDeviceId(userId, deviceId);
        } else {
            refreshTokenRepository.revokeAllByUserId(userId);
            deviceTokenRepository.deactivateAllByUserId(userId);
        }
    }

    private void saveRefreshToken(AuthUser user, String rawToken) {
        String tokenHash = jwtTokenProvider.hashToken(rawToken);
        AuthRefreshToken refreshToken = AuthRefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        // Redis 캐시에도 저장
        cacheRefreshToken(tokenHash, user.getUserId());
    }

    private void cacheRefreshToken(String tokenHash, Long userId) {
        try {
            redisTemplate.opsForValue().set(RT_CACHE_PREFIX + tokenHash, userId.toString(), RT_CACHE_TTL);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패: {}", e.getMessage());
        }
    }

    private void evictRefreshTokenCache(String tokenHash) {
        try {
            redisTemplate.delete(RT_CACHE_PREFIX + tokenHash);
        } catch (Exception e) {
            log.warn("Redis 캐시 삭제 실패: {}", e.getMessage());
        }
    }

    private void evictAllRefreshTokenCache(Long userId) {
        try {
            var tokens = refreshTokenRepository.findAllByUser_UserIdAndIsRevokedFalse(userId);
            tokens.forEach(t -> redisTemplate.delete(RT_CACHE_PREFIX + t.getTokenHash()));
        } catch (Exception e) {
            log.warn("Redis 캐시 전체 삭제 실패: {}", e.getMessage());
        }
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token");
    }
}
