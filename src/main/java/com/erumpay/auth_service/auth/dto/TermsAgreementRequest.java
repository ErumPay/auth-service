package com.erumpay.auth_service.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TermsAgreementRequest {

    @NotNull(message = "서비스이용약관 동의 여부는 필수입니다")
    private Boolean serviceTermsAgreed;

    @NotNull(message = "개인정보처리방침 동의 여부는 필수입니다")
    private Boolean privacyTermsAgreed;

    private Boolean marketingTermsAgreed;
}
