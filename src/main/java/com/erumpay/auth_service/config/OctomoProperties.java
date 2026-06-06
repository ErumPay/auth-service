package com.erumpay.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "octomo")
public class OctomoProperties {
    @NotBlank
    private String apiUrl;

    @NotBlank
    private String apiKey;

    private String receiverNumber;
}
