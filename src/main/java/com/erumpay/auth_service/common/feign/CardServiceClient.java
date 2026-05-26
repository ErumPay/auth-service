package com.erumpay.auth_service.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "card-service", url = "${feign.card-service.url:http://localhost:8083}")
public interface CardServiceClient {

    @PostMapping("/api/v1/internal/cards/users/{userId}/deactivate-billing-keys")
    Map<String, Object> deactivateBillingKeys(@PathVariable Long userId);
}
