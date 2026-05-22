package com.erumpay.auth_service.pin.service;

import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.util.PinHashUtil;
import com.erumpay.auth_service.pin.dto.PinChangeRequest;
import com.erumpay.auth_service.pin.dto.PinResetRequest;
import com.erumpay.auth_service.pin.dto.PinSetupRequest;
import com.erumpay.auth_service.pin.entity.AuthPin;
import com.erumpay.auth_service.pin.repository.AuthPinRepository;
import com.erumpay.auth_service.sms.entity.AuthSmsVerification;
import com.erumpay.auth_service.sms.repository.AuthSmsVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PinService {

    private final AuthPinRepository pinRepository;
    private final AuthUserRepository userRepository;
    private final AuthSmsVerificationRepository smsRepository;
    private final PinHashUtil pinHashUtil;

    // 04. PIN 최초설정
    @Transactional
    public Map<String, String> setupPin(Long userId, PinSetupRequest request) {
        if (!request.getPin().equals(request.getPinConfirm())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PIN 불일치 (pin ≠ pinConfirm)");
        }
        validatePinPattern(request.getPin());

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // SMS 인증 완료 확인
        if (!smsRepository.existsByUser_UserIdAndIsVerifiedTrueAndDeletedAtIsNull(userId)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "SMS 본인인증 미완료");
        }

        if (pinRepository.existsByUser_UserIdAndDeletedAtIsNull(userId)) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 PIN이 설정됨");
        }

        String salt = pinHashUtil.generateSalt();
        String hash = pinHashUtil.hashPin(request.getPin(), salt);

        AuthPin pin = AuthPin.builder()
                .user(user)
                .pinHash(hash)
                .pinSalt(salt)
                .failCount(0)
                .build();
        pinRepository.save(pin);

        // PENDING → ACTIVE 전환
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }

        return Map.of("message", "PIN 설정 완료");
    }

    // 05. PIN 변경
    @Transactional(noRollbackFor = AuthException.class)
    public Map<String, String> changePin(Long userId, PinChangeRequest request) {
        if (!request.getNewPin().equals(request.getNewPinConfirm())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "새 PIN 불일치 (newPin ≠ newPinConfirm)");
        }
        validatePinPattern(request.getNewPin());

        AuthPin pin = getActivePin(userId);
        checkLock(pin);

        if (!pinHashUtil.verifyPin(request.getCurrentPin(), pin.getPinSalt(), pin.getPinHash())) {
            handleFailedAttempt(pin);
            throw new AuthException(HttpStatus.UNAUTHORIZED, "현재 PIN 불일치");
        }

        String newSalt = pinHashUtil.generateSalt();
        pin.setPinHash(pinHashUtil.hashPin(request.getNewPin(), newSalt));
        pin.setPinSalt(newSalt);
        pin.setFailCount(0);
        pin.setFailLastAt(null);
        pin.setLockedUntil(null);

        return Map.of("message", "PIN 변경 완료");
    }

    // 22. PIN 재설정 (10회 실패 후 SMS 재인증)
    @Transactional
    public Map<String, String> resetPin(Long userId, PinResetRequest request) {
        if (!request.getNewPin().equals(request.getNewPinConfirm())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "새 PIN 불일치 (newPin ≠ newPinConfirm)");
        }
        validatePinPattern(request.getNewPin());

        AuthPin pin = getActivePin(userId);

        if (pin.getFailCount() < 10) {
            throw new AuthException(HttpStatus.FORBIDDEN, "PIN 재설정 대상이 아님 (fail_count < 10)");
        }

        // SMS 인증 완료 확인
        AuthSmsVerification sms = smsRepository
                .findByVerificationIdAndIsVerifiedTrueAndDeletedAtIsNull(request.getVerificationId())
                .orElseThrow(() -> new AuthException(HttpStatus.FORBIDDEN, "SMS 재인증 미완료 상태"));

        String newSalt = pinHashUtil.generateSalt();
        pin.setPinHash(pinHashUtil.hashPin(request.getNewPin(), newSalt));
        pin.setPinSalt(newSalt);
        pin.setFailCount(0);
        pin.setFailLastAt(null);
        pin.setLockedUntil(null);

        return Map.of("message", "PIN 재설정 완료");
    }

    // 11. 내부 PIN 검증 (payment-service에서 호출)
    @Transactional(noRollbackFor = AuthException.class)
    public Map<String, Object> verifyPin(Long userId, String pin) {
        AuthPin authPin = getActivePin(userId);

        // 24시간 경과 시 fail_count 초기화
        if (authPin.getFailLastAt() != null
                && authPin.getFailLastAt().plusHours(24).isBefore(LocalDateTime.now())) {
            authPin.setFailCount(0);
            authPin.setFailLastAt(null);
            authPin.setLockedUntil(null);
        }

        if (authPin.requiresSmsVerification()) {
            throw new AuthException(HttpStatus.LOCKED, "PIN 재설정 필요 (10회 실패)",
                    Map.of("requireSmsVerification", true));
        }

        checkLock(authPin);

        if (!pinHashUtil.verifyPin(pin, authPin.getPinSalt(), authPin.getPinHash())) {
            handleFailedAttempt(authPin);
            int remain = 5 - (authPin.getFailCount() % 5);
            if (remain <= 0) remain = 5;
            throw new AuthException(HttpStatus.UNAUTHORIZED, "PIN 불일치",
                    Map.of("failCount", authPin.getFailCount(), "remainCount", remain));
        }

        // 성공 시 초기화
        authPin.setFailCount(0);
        authPin.setFailLastAt(null);
        authPin.setLockedUntil(null);

        return Map.of("verified", true);
    }

    private AuthPin getActivePin(Long userId) {
        return pinRepository.findByUser_UserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "PIN이 설정되지 않음"));
    }

    private void checkLock(AuthPin pin) {
        if (pin.isLocked()) {
            throw new AuthException(HttpStatus.LOCKED, "PIN 잠금 상태",
                    Map.of("lockedUntil", pin.getLockedUntil().toString()));
        }
    }

    private void handleFailedAttempt(AuthPin pin) {
        pin.setFailCount(pin.getFailCount() + 1);
        pin.setFailLastAt(LocalDateTime.now());

        if (pin.getFailCount() >= 10) {
            // 10회: SMS 재인증 필요 상태로 유지
        } else if (pin.getFailCount() % 5 == 0) {
            // 5회 배수: 5분 잠금
            pin.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        }
    }

    private void validatePinPattern(String pin) {
        if (pinHashUtil.isWeakPattern(pin)) {
            throw new AuthException(HttpStatus.BAD_REQUEST,
                    "취약 패턴 — 연속 숫자 또는 동일 숫자 4자리 이상");
        }
    }
}
