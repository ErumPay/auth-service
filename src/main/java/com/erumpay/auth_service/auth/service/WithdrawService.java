package com.erumpay.auth_service.auth.service;

import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthRefreshTokenRepository;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.feign.CardServiceClient;
import com.erumpay.auth_service.common.feign.PaymentServiceClient;
import com.erumpay.auth_service.common.kafka.AuthEventProducer;
import com.erumpay.auth_service.device.repository.AuthDeviceTokenRepository;
import com.erumpay.auth_service.friend.entity.FriendRelation;
import com.erumpay.auth_service.friend.entity.FriendRelation.FriendStatus;
import com.erumpay.auth_service.friend.repository.FriendRelationRepository;
import com.erumpay.auth_service.invite.repository.FriendAddLinkRepository;
import com.erumpay.auth_service.pin.entity.AuthPin;
import com.erumpay.auth_service.pin.repository.AuthPinRepository;
import com.erumpay.auth_service.pin.service.PinService;
import com.erumpay.auth_service.sms.repository.AuthSmsVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawService {

    private final AuthUserRepository userRepository;
    private final AuthRefreshTokenRepository refreshTokenRepository;
    private final AuthDeviceTokenRepository deviceTokenRepository;
    private final AuthPinRepository pinRepository;
    private final AuthSmsVerificationRepository smsRepository;
    private final FriendRelationRepository friendRelationRepository;
    private final FriendAddLinkRepository friendAddLinkRepository;
    private final PinService pinService;
    private final PaymentServiceClient paymentServiceClient;
    private final CardServiceClient cardServiceClient;
    private final AuthEventProducer authEventProducer;

    @Transactional
    public Map<String, String> withdraw(Long userId, String pin) {
        // 1. PIN 검증
        pinService.verifyPin(userId, pin);

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // 2. 미정산 거래 확인 (payment-service)
        try {
            Map<String, Object> pending = paymentServiceClient.checkPendingTransactions(userId);
            Boolean hasPending = (Boolean) pending.get("hasPending");
            if (Boolean.TRUE.equals(hasPending)) {
                String reason = (String) pending.getOrDefault("reason", "진행 중인 거래 존재");
                throw new AuthException(HttpStatus.CONFLICT, reason);
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.warn("payment-service 호출 실패, 탈퇴 계속 진행: {}", e.getMessage());
        }

        // 3. 빌링키 비활성화 (card-service)
        try {
            cardServiceClient.deactivateBillingKeys(userId);
        } catch (Exception e) {
            log.warn("card-service 빌링키 비활성화 실패: {}", e.getMessage());
        }

        // 4. 소프트 삭제
        LocalDateTime now = LocalDateTime.now();

        user.setStatus(UserStatus.WITHDRAWN);
        user.setWithdrawnAt(now);

        // auth_pin: deleted_at
        pinRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .ifPresent(p -> p.setDeletedAt(now));

        // auth_refresh_tokens: is_revoked
        refreshTokenRepository.revokeAllByUserId(userId);

        // auth_device_tokens: is_active = false
        deviceTokenRepository.deactivateAllByUserId(userId);

        // auth_sms_verifications: deleted_at
        smsRepository.softDeleteByUserId(userId);

        // friend_relations: DELETED
        List<FriendRelation> relations = friendRelationRepository.findByUser_UserIdOrFriend_UserId(userId, userId);
        relations.forEach(r -> r.setStatus(FriendStatus.DELETED));

        // friend_add_links: is_used = true
        friendAddLinkRepository.markAllUsedByInviterId(userId);

        // 5. Kafka 이벤트 발행
        authEventProducer.sendUserWithdrawn(userId);

        return Map.of("message", "탈퇴 처리 완료");
    }
}
