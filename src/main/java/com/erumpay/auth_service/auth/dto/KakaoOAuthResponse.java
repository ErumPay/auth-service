package com.erumpay.auth_service.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class KakaoOAuthResponse {
    private boolean isNewUser;
    private Long userId;
    private String name;
    private String accessToken;
    private String refreshToken;
    private String status;
}
