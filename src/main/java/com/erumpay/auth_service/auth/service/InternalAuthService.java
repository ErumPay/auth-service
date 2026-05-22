package com.erumpay.auth_service.auth.service;

import com.erumpay.auth_service.auth.JwtTokenProvider;
import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InternalAuthService {

    private final AuthUserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AesEncryptionUtil aesEncryptionUtil;

    // 10. JWT 검증
    public Map<String, Object> validateJwt(String bearerToken) {
        String token = extractToken(bearerToken);

        if (!jwtTokenProvider.validateToken(token)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
        }

        Long userId = jwtTokenProvider.getUserId(token);
        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthException(HttpStatus.FORBIDDEN,
                    "사용자 상태가 ACTIVE가 아님 (SUSPENDED 또는 WITHDRAWN)");
        }

        return Map.of("userId", user.getUserId(), "status", user.getStatus().name());
    }

    // 13. 사용자 정보 조회 (단건)
    public Map<String, Object> getUserInfo(Long userId) {
        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "해당 사용자 없음"));

        return buildUserInfoMap(user);
    }

    // 13. 사용자 정보 조회 (복수)
    public List<Map<String, Object>> getUserInfoBatch(List<Long> userIds) {
        return userRepository.findByUserIdIn(userIds).stream()
                .map(this::buildUserInfoMap)
                .toList();
    }

    private Map<String, Object> buildUserInfoMap(AuthUser user) {
        String phoneNumber = user.getPhoneNumber() != null ? aesEncryptionUtil.decrypt(user.getPhoneNumber()) : null;
        String birthDate = user.getBirthDate() != null ? aesEncryptionUtil.decrypt(user.getBirthDate()) : null;

        return Map.of(
                "userId", user.getUserId(),
                "name", user.getName() != null ? user.getName() : "",
                "phoneNumber", phoneNumber != null ? phoneNumber : "",
                "birthDate", birthDate != null ? birthDate : "",
                "status", user.getStatus().name()
        );
    }

    private String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
    }
}
