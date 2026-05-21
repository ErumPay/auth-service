package com.erumpay.auth_service.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class AuthEventProducer {

    private static final String TOPIC = "auth.event";

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserWithdrawn(Long userId) {
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate 미설정 — USER_WITHDRAWN 이벤트 생략 (userId={})", userId);
            return;
        }
        Map<String, Object> event = Map.of(
                "eventType", "USER_WITHDRAWN",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
        );
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(userId), event);
            log.info("Kafka 이벤트 발행: USER_WITHDRAWN, userId={}", userId);
        } catch (Exception e) {
            log.warn("Kafka 이벤트 발행 실패 (USER_WITHDRAWN): {}", e.getMessage());
        }
    }

    public void sendFriendRequest(Long fromUserId, Long toUserId) {
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate 미설정 — AUTH_FRIEND 이벤트 생략 (from={}, to={})", fromUserId, toUserId);
            return;
        }
        Map<String, Object> event = Map.of(
                "eventType", "AUTH_FRIEND",
                "fromUserId", fromUserId,
                "toUserId", toUserId,
                "timestamp", System.currentTimeMillis()
        );
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(toUserId), event);
            log.info("Kafka 이벤트 발행: AUTH_FRIEND, from={} to={}", fromUserId, toUserId);
        } catch (Exception e) {
            log.warn("Kafka 이벤트 발행 실패 (AUTH_FRIEND): {}", e.getMessage());
        }
    }
}
