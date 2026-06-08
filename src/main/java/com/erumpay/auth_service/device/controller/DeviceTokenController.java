package com.erumpay.auth_service.device.controller;

import com.erumpay.auth_service.device.dto.DeviceTokenRequest;
import com.erumpay.auth_service.device.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/device")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    // 09. FCM 디바이스토큰 등록/갱신
    @PostMapping("/token")
    public ResponseEntity<Map<String, String>> registerToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DeviceTokenRequest request) {
        return ResponseEntity.ok(deviceTokenService.registerToken(userId, request));
    }
}
