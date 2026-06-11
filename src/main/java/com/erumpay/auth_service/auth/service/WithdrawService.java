package com.erumpay.auth_service.auth.service;

import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthRefreshTokenRepository;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.feign.CardServiceClient;
import com.erumpay.auth_service.common.feign.PaymentServiceClient;
import com.erumpay.auth_service.common.feign.dto.InternalDeactivateCardsResponse;
import com.erumpay.auth_service.common.feign.dto.UserWithdrawalResponse;
import com.erumpay.auth_service.common.kafka.AuthEventProducer;
import com.erumpay.auth_service.device.repository.AuthDeviceTokenRepository;
import com.erumpay.auth_service.friend.entity.FriendRelation;
import com.erumpay.auth_service.friend.entity.FriendRelation.FriendStatus;
import com.erumpay.auth_service.friend.repository.FriendRelationRepository;
import com.erumpay.auth_service.invite.repository.FriendAddLinkRepository;
import com.erumpay.auth_service.pin.repository.AuthPinRepository;
import com.erumpay.auth_service.pin.service.PinService;
import com.erumpay.auth_service.sms.repository.AuthSmsVerificationRepository;
import feign.FeignException;
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

    @Transactional(readOnly = true)
    public UserWithdrawalResponse checkWithdrawalEligibility(Long userId) {
        return callPaymentWithdrawalValidation(userId);
    }

    @Transactional
    public Map<String, String> withdraw(Long userId, String pin) {
        // 1. PIN 검증
        pinService.verifyPin(userId, pin);

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // 2. idempotency 가드 — 이미 탈퇴된 계정 차단
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 탈퇴된 계정입니다.");
        }

        // 3. 미정산 거래 확인 (payment-service)
        UserWithdrawalResponse validation = callPaymentWithdrawalValidation(userId);
        if (!validation.isPossibility()) {
            String reason = validation.getMessage() != null
                    ? validation.getMessage()
                    : "미결제 또는 처리 중인 결제 건이 있어 탈퇴할 수 없습니다.";
            throw new AuthException(HttpStatus.CONFLICT, reason);
        }

        // 4. 카드/빌링키 비활성화 (card-service)
        callCardDeactivateAll(userId);

        // 5. 소프트 삭제
        LocalDateTime now = LocalDateTime.now();

        user.setStatus(UserStatus.WITHDRAWN);
        user.setWithdrawnAt(now);

        pinRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .ifPresent(p -> p.setDeletedAt(now));
        refreshTokenRepository.revokeAllByUserId(userId);
        deviceTokenRepository.deactivateAllByUserId(userId);
        smsRepository.softDeleteByUserId(userId);

        List<FriendRelation> relations = friendRelationRepository.findByUser_UserIdOrFriend_UserId(userId, userId);
        relations.forEach(r -> r.setStatus(FriendStatus.DELETED));
        friendAddLinkRepository.markAllUsedByInviterId(userId);

        // 6. Kafka 이벤트 발행
        authEventProducer.sendUserWithdrawn(userId);

        return Map.of("message", "탈퇴 처리 완료");
    }

    private UserWithdrawalResponse callPaymentWithdrawalValidation(Long userId) {
        try {
            return paymentServiceClient.getWithdrawalValidation(userId);
        } catch (FeignException e) {
            log.warn("payment-service withdrawal-validation 호출 실패. status={}, message={}",
                    e.status(), e.getMessage());
            throw new AuthException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "결제 서비스 일시 장애. 잠시 후 다시 시도해주세요."
            );
        }
    }

    private void callCardDeactivateAll(Long userId) {
        try {
            InternalDeactivateCardsResponse response = cardServiceClient.deactivateAll(userId);
            log.info("card-service deactivate-all 완료. userId={}, deactivatedCount={}",
                    userId, response != null ? response.getDeactivatedCount() : null);
        } catch (FeignException e) {
            log.warn("card-service deactivate-all 호출 실패. status={}, message={}",
                    e.status(), e.getMessage());
            throw new AuthException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "카드 서비스 일시 장애. 잠시 후 다시 시도해주세요."
            );
        }
    }
}
