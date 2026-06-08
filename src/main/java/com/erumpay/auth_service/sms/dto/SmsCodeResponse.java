package com.erumpay.auth_service.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SmsCodeResponse {
    private Long verificationId;
    private String smsReceiverNumber;
    private String verificationCode;
    private LocalDateTime expiresAt;
}
