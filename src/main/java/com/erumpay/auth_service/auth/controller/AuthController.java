package com.erumpay.auth_service.auth.controller;

import com.erumpay.auth_service.auth.dto.*;
import com.erumpay.auth_service.auth.service.AuthService;
import com.erumpay.auth_service.auth.service.WithdrawService;
import com.erumpay.auth_service.common.feign.dto.UserWithdrawalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final WithdrawService withdrawService;

    // 01. 카카오 OAuth 회원가입/로그인
    @PostMapping("/kakao/login")
    public ResponseEntity<KakaoOAuthResponse> kakaoLogin(@Valid @RequestBody KakaoOAuthRequest request) {
        return ResponseEntity.ok(authService.kakaoOAuthLogin(request));
    }

    // 02. 약관 동의 (카카오 로그인 후 신규 회원)
    @PostMapping("/terms/agree")
    public ResponseEntity<Map<String, String>> agreeTerms(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TermsAgreementRequest request) {
        authService.agreeTerms(userId, request.getServiceTermsAgreed(), request.getPrivacyTermsAgreed(), request.getMarketingTermsAgreed());
        return ResponseEntity.ok(Map.of("message", "약관 동의 완료"));
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

    // 08-1. 회원탈퇴 가능 여부 조회 (모바일 사전 검증용)
    @GetMapping("/withdraw/eligibility")
    public ResponseEntity<UserWithdrawalResponse> getWithdrawalEligibility(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(withdrawService.checkWithdrawalEligibility(userId));
    }
}
