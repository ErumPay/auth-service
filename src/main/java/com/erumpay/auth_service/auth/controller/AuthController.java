package com.erumpay.auth_service.auth.controller;

import com.erumpay.auth_service.auth.dto.*;
import com.erumpay.auth_service.auth.service.AuthService;
import com.erumpay.auth_service.auth.service.WithdrawService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final WithdrawService withdrawService;

    // 01. 카카오 OAuth 회원가입/로그인
    @PostMapping("/kakao/login")
    public ResponseEntity<KakaoOAuthResponse> kakaoLogin(@Valid @RequestBody KakaoOAuthRequest request) {
        return ResponseEntity.ok(authService.kakaoOAuthLogin(request));
    }

    // 06. Access Token 재발급
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(authService.refreshToken(authorization));
    }

    // 07. 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) LogoutRequest request) {
        String deviceId = request != null ? request.getDeviceId() : null;
        authService.logout(userId, deviceId);
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }

    // 08. 회원탈퇴
    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(withdrawService.withdraw(userId, request.getPin()));
    }
}
