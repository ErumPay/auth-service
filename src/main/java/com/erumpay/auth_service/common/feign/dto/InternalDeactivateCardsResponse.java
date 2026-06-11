package com.erumpay.auth_service.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalDeactivateCardsResponse {

    private Long userId;
    private Integer deactivatedCount;
}
