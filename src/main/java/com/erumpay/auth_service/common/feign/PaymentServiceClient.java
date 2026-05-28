package com.erumpay.auth_service.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "payment-service", url = "${feign.payment-service.url:http://localhost:8082}")
public interface PaymentServiceClient {

    @GetMapping("/internal/v1/payments/users/{userId}/pending")
    Map<String, Object> checkPendingTransactions(@PathVariable Long userId);
}
