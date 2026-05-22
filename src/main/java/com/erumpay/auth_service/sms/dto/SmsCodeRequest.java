package com.erumpay.auth_service.sms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsCodeRequest {

    @NotBlank(message = "전화번호는 필수입니다")
    private String phoneNumber;
}
