package com.erumpay.auth_service.sms.controller;

import com.erumpay.auth_service.sms.dto.SmsCodeRequest;
import com.erumpay.auth_service.sms.dto.SmsCodeResponse;
import com.erumpay.auth_service.sms.dto.SmsVerifyRequest;
import com.erumpay.auth_service.sms.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    // 02. SMS 인증코드 생성
    @PostMapping("/send")
    public ResponseEntity<SmsCodeResponse> generateCode(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SmsCodeRequest request) {
        return ResponseEntity.ok(smsService.generateCode(userId, request));
    }

    // 03. SMS 인증코드 확인
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Boolean>> verifyCode(@Valid @RequestBody SmsVerifyRequest request) {
        return ResponseEntity.ok(smsService.verifyCode(request));
    }
}
