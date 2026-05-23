package com.erumpay.auth_service.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteRequest {

    @NotNull(message = "즐겨찾기 여부는 필수입니다")
    private Boolean isFavorite;
}
