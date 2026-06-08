package com.erumpay.auth_service.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoUserInfo {
    private String kakaoOauthId;
    private String name;
    private String phoneNumber;
    private String birthDate;
}
