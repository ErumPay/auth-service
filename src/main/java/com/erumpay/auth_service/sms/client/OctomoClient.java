package com.erumpay.auth_service.sms.client;

import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.config.OctomoProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OctomoClient {

    private final OctomoProperties octomoProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean existsMessage(String mobileNum, String text) {
        if (octomoProperties.getApiKey() == null || octomoProperties.getApiKey().isBlank()) {
            throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "Octomo API Key가 설정되지 않음");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Octomo " + octomoProperties.getApiKey());

        HttpEntity<Map<String, String>> request = new HttpEntity<>(
                Map.of("mobileNum", mobileNum, "text", text),
                headers
        );

        try {
            log.info("[Octomo] 문자 조회 요청 - mobileNum: {}, text: {}", mobileNum, text);
            ResponseEntity<OctomoExistsResponse> response = restTemplate.postForEntity(
                    octomoProperties.getApiUrl(),
                    request,
                    OctomoExistsResponse.class
            );

            OctomoExistsResponse body = response.getBody();
            boolean verified = body != null && body.isVerified();
            log.info("[Octomo] 문자 조회 응답 - status: {}, verified: {}", response.getStatusCode(), verified);
            return verified;
        } catch (RestClientException e) {
            log.error("Octomo 문자 조회 API 호출 실패", e);
            throw new AuthException(HttpStatus.BAD_GATEWAY, "Octomo API 호출 오류");
        }
    }

    @Getter
    @Setter
    public static class OctomoExistsResponse {
        private Boolean exists;
        private Boolean verified;

        public boolean isVerified() {
            return Boolean.TRUE.equals(exists) || Boolean.TRUE.equals(verified);
        }
    }
}
