package com.erumpay.auth_service.auth.controller;

import com.erumpay.auth_service.auth.JwtTokenProvider;
import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevTokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthUserRepository authUserRepository;

    @GetMapping("/token/{userId}")
    public ResponseEntity<Map<String, String>> issueToken(@PathVariable Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getStatus().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken
        ));
    }
}
