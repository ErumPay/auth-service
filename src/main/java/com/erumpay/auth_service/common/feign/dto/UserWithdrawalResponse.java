package com.erumpay.auth_service.common.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWithdrawalResponse {

    private boolean possibility;
    private Long userId;
    private boolean hasUnpaidPayments;
    private Long unpaidPaymentCount;
    private String message;
}
