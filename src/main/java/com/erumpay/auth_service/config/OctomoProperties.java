package com.erumpay.auth_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "octomo")
public class OctomoProperties {
    private String apiUrl;
    private String apiKey;
    private String receiverNumber;
}
