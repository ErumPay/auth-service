package com.erumpay.auth_service.common.feign;

import com.erumpay.auth_service.common.feign.dto.InternalDeactivateCardsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "card-service", url = "${feign.card-service.url:http://localhost:8082}")
public interface CardServiceClient {

    @PostMapping("/internal/v1/cards/users/{userId}/deactivate-all")
    InternalDeactivateCardsResponse deactivateAll(@PathVariable Long userId);
}
