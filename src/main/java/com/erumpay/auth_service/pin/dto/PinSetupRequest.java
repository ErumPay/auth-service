package com.erumpay.auth_service.pin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinSetupRequest {

    @NotBlank(message = "PIN은 필수입니다")
    @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자만 허용")
    private String pin;

    @NotBlank(message = "PIN 확인은 필수입니다")
    @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자만 허용")
    private String pinConfirm;
}
