package com.erumpay.auth_service.auth.service;

import com.erumpay.auth_service.auth.dto.KakaoUserInfo;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.config.KakaoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthService {

    private final KakaoProperties kakaoProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoProperties.getClientId());
        params.add("redirect_uri", kakaoProperties.getRedirectUri());
        params.add("code", authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoProperties.getTokenUrl(), request, Map.class);
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            log.error("카카오 토큰 발급 실패", e);
            throw new AuthException(HttpStatus.BAD_GATEWAY, "카카오 API 호출 오류");
        }
    }

    @SuppressWarnings("unchecked")
    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(kakaoAccessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoProperties.getUserInfoUrl(), HttpMethod.GET, request, Map.class);

            Map<String, Object> body = response.getBody();
            Map<String, Object> kakaoAccount = (Map<String, Object>) body.get("kakao_account");

            KakaoUserInfo userInfo = new KakaoUserInfo();
            userInfo.setKakaoOauthId(String.valueOf(body.get("id")));

            if (kakaoAccount != null) {
                userInfo.setName((String) kakaoAccount.get("name"));
                userInfo.setPhoneNumber(normalizePhoneNumber((String) kakaoAccount.get("phone_number")));
                userInfo.setBirthDate(extractBirthDate(kakaoAccount));
            }

            return userInfo;
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new AuthException(HttpStatus.BAD_GATEWAY, "카카오 API 호출 오류");
        }
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9]", "")
                .replaceFirst("^82", "0");
    }

    private String extractBirthDate(Map<String, Object> kakaoAccount) {
        String birthday = (String) kakaoAccount.get("birthday");
        String birthyear = (String) kakaoAccount.get("birthyear");
        if (birthyear != null && birthday != null) {
            return birthyear + birthday;
        }
        return birthday;
    }
}
