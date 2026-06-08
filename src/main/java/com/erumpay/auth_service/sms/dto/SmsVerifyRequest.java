package com.erumpay.auth_service.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsVerifyRequest {

    @NotNull(message = "인증 ID는 필수입니다")
    private Long verificationId;

    @NotBlank(message = "인증번호는 필수입니다")
    private String code;
}
