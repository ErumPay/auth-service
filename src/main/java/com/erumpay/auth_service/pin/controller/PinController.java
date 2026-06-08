package com.erumpay.auth_service.pin.controller;

import com.erumpay.auth_service.pin.dto.PinChangeRequest;
import com.erumpay.auth_service.pin.dto.PinResetRequest;
import com.erumpay.auth_service.pin.dto.PinSetupRequest;
import com.erumpay.auth_service.pin.service.PinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/pin")
@RequiredArgsConstructor
public class PinController {

    private final PinService pinService;

    // 04. PIN 최초설정
    @PostMapping("/setup")
    public ResponseEntity<Map<String, String>> setupPin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PinSetupRequest request) {
        return ResponseEntity.ok(pinService.setupPin(userId, request));
    }

    // 05. PIN 변경
    @PutMapping("/change")
    public ResponseEntity<Map<String, String>> changePin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PinChangeRequest request) {
        return ResponseEntity.ok(pinService.changePin(userId, request));
    }

    // 22. PIN 재설정
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetPin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PinResetRequest request) {
        return ResponseEntity.ok(pinService.resetPin(userId, request));
    }
}
