package com.erumpay.auth_service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoOAuthRequest {

    @NotBlank(message = "인가 코드는 필수입니다")
    private String authorizationCode;

    @NotNull(message = "서비스이용약관 동의 여부는 필수입니다")
    private Boolean serviceTermsAgreed;

    @NotNull(message = "개인정보처리방침 동의 여부는 필수입니다")
    private Boolean privacyTermsAgreed;

    private Boolean marketingTermsAgreed;

    private String redirectUri;
}
