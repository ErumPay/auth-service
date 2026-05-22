package com.erumpay.auth_service.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceTokenRequest {

    @NotBlank(message = "fcmToken은 필수입니다")
    private String fcmToken;

    @NotNull(message = "deviceOs는 필수입니다")
    private String deviceOs;

    @NotBlank(message = "deviceId는 필수입니다")
    private String deviceId;
}
