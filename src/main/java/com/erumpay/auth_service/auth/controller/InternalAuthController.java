package com.erumpay.auth_service.auth.controller;

import com.erumpay.auth_service.auth.service.InternalAuthService;
import com.erumpay.auth_service.device.service.DeviceTokenService;
import com.erumpay.auth_service.friend.service.FriendService;
import com.erumpay.auth_service.pin.service.PinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final InternalAuthService internalAuthService;
    private final PinService pinService;
    private final DeviceTokenService deviceTokenService;
    private final FriendService friendService;

    // 10. JWT 검증
    @GetMapping("/auth/validate")
    public ResponseEntity<Map<String, Object>> validateJwt(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(internalAuthService.validateJwt(authorization));
    }

    // 11. PIN 검증
    @PostMapping("/auth/pin/verify")
    public ResponseEntity<Map<String, Object>> verifyPin(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        String pin = (String) request.get("pin");
        return ResponseEntity.ok(pinService.verifyPin(userId, pin));
    }

    // 12. FCM 토큰 조회
    @GetMapping("/users/{userId}/device-tokens")
    public ResponseEntity<Map<String, Object>> getDeviceTokens(@PathVariable Long userId) {
        List<Map<String, String>> tokens = deviceTokenService.getActiveTokens(userId);
        return ResponseEntity.ok(Map.of("tokens", tokens));
    }

    // 13. 사용자 정보 조회 (단건)
    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable Long userId) {
        return ResponseEntity.ok(internalAuthService.getUserInfo(userId));
    }

    // 13. 사용자 정보 조회 (복수)
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getUserInfoBatch(
            @RequestParam List<Long> userIds) {
        return ResponseEntity.ok(internalAuthService.getUserInfoBatch(userIds));
    }

    // 25. 내부 FCM 토큰 비활성화
    @PatchMapping("/device-tokens/deactivate")
    public ResponseEntity<Map<String, String>> deactivateDeviceToken(@RequestBody Map<String, String> request) {
        String fcmToken = request.get("fcmToken");
        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "fcmToken 누락"));
        }
        return ResponseEntity.ok(deviceTokenService.deactivateToken(fcmToken));
    }

    // 24. 친구 관계 확인
    @GetMapping("/friends/check")
    public ResponseEntity<Map<String, Boolean>> checkFriendship(
            @RequestParam Long userId,
            @RequestParam Long friendUserId) {
        return ResponseEntity.ok(Map.of("isFriend", friendService.isFriend(userId, friendUserId)));
    }
}
