package com.erumpay.auth_service.common.feign;

import com.erumpay.auth_service.common.feign.dto.UserWithdrawalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment-service", url = "${feign.payment-service.url:http://localhost:8083}")
public interface PaymentServiceClient {

    @GetMapping("/internal/v1/payments/users/{userId}/withdrawal-validation")
    UserWithdrawalResponse getWithdrawalValidation(@PathVariable Long userId);
}
