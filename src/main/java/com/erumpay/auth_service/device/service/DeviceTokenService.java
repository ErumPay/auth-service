package com.erumpay.auth_service.device.service;

import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.device.dto.DeviceTokenRequest;
import com.erumpay.auth_service.device.entity.AuthDeviceToken;
import com.erumpay.auth_service.device.entity.AuthDeviceToken.DeviceOs;
import com.erumpay.auth_service.device.repository.AuthDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final AuthDeviceTokenRepository deviceTokenRepository;
    private final AuthUserRepository userRepository;

    // 09. FCM 디바이스토큰 등록/갱신
    @Transactional
    public Map<String, String> registerToken(Long userId, DeviceTokenRequest request) {
        DeviceOs deviceOs;
        try {
            deviceOs = DeviceOs.valueOf(request.getDeviceOs().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "유효하지 않은 OS 타입 — ANDROID, IOS만 허용");
        }

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // 다른 사용자가 같은 fcmToken 사용 중이면 비활성화
        Optional<AuthDeviceToken> existingByFcm = deviceTokenRepository.findByFcmToken(request.getFcmToken());
        existingByFcm.ifPresent(token -> {
            if (!token.getUser().getUserId().equals(userId)) {
                token.setIsActive(false);
            }
        });

        // upsert: 같은 (user_id, device_id)면 갱신
        Optional<AuthDeviceToken> existingByDevice = deviceTokenRepository
                .findByUser_UserIdAndDeviceId(userId, request.getDeviceId());

        if (existingByDevice.isPresent()) {
            AuthDeviceToken token = existingByDevice.get();
            token.setFcmToken(request.getFcmToken());
            token.setDeviceOs(deviceOs);
            token.setIsActive(true);
        } else {
            AuthDeviceToken newToken = AuthDeviceToken.builder()
                    .user(user)
                    .fcmToken(request.getFcmToken())
                    .deviceOs(deviceOs)
                    .deviceId(request.getDeviceId())
                    .isActive(true)
                    .build();
            deviceTokenRepository.save(newToken);
        }

        return Map.of("message", "토큰 등록 완료");
    }

    // 25. 내부 FCM 토큰 비활성화
    @Transactional
    public Map<String, String> deactivateToken(String fcmToken) {
        AuthDeviceToken token = deviceTokenRepository.findByFcmToken(fcmToken)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "해당 FCM 토큰을 찾을 수 없음"));

        token.setIsActive(false);
        return Map.of("message", "디바이스 토큰 비활성화 완료");
    }

    // 12. 내부 FCM 토큰 조회
    public List<Map<String, String>> getActiveTokens(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new AuthException(HttpStatus.NOT_FOUND, "해당 사용자 없음");
        }

        return deviceTokenRepository.findByUser_UserIdAndIsActiveTrue(userId)
                .stream()
                .map(t -> Map.of("fcmToken", t.getFcmToken(), "deviceOs", t.getDeviceOs().name()))
                .toList();
    }
}
