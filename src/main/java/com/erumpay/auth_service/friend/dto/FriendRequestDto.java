package com.erumpay.auth_service.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestDto {

    @NotNull(message = "친구 사용자 ID는 필수입니다")
    private Long friendUserId;
}
