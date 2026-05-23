package com.erumpay.auth_service.sms.service;

import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.util.AesEncryptionUtil;
import com.erumpay.auth_service.sms.dto.SmsCodeRequest;
import com.erumpay.auth_service.sms.dto.SmsCodeResponse;
import com.erumpay.auth_service.sms.dto.SmsVerifyRequest;
import com.erumpay.auth_service.sms.entity.AuthSmsVerification;
import com.erumpay.auth_service.sms.repository.AuthSmsVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final AuthSmsVerificationRepository smsRepository;
    private final AuthUserRepository userRepository;
    private final AesEncryptionUtil aesEncryptionUtil;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String RECEIVER_NUMBER = "1666-3538";

    @Transactional
    public SmsCodeResponse generateCode(Long userId, SmsCodeRequest request) {
        log.info("[SMS] generateCode 진입 - userId: {}, phoneNumber: {}", userId, request.getPhoneNumber());
        String phone = request.getPhoneNumber();
        if (!phone.matches("^01[016789]\\d{7,8}$")) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "유효하지 않은 전화번호 형식");
        }

        // 429 동일 전화번호 중복 요청 체크
        String phoneHash = aesEncryptionUtil.hashWithFixedSalt(phone);
        if (smsRepository.existsValidRequestByPhoneHash(phoneHash, LocalDateTime.now())) {
            throw new AuthException(HttpStatus.TOO_MANY_REQUESTS, "이미 발송된 인증번호가 유효합니다. 3분 후 재시도해주세요.");
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(3);

        AuthSmsVerification verification = AuthSmsVerification.builder()
                .user(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .phoneNumber(aesEncryptionUtil.encrypt(phone))
                .phoneNumberHash(phoneHash)
                .verificationCode(code)
                .isVerified(false)
                .expiresAt(expiresAt)
                .build();

        smsRepository.save(verification);

        return SmsCodeResponse.builder()
                .verificationId(verification.getVerificationId())
                .smsReceiverNumber(RECEIVER_NUMBER)
                .verificationCode(code)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public Map<String, Boolean> verifyCode(SmsVerifyRequest request) {
        AuthSmsVerification verification = smsRepository
                .findByVerificationIdAndIsVerifiedFalseAndDeletedAtIsNull(request.getVerificationId())
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없음"));

        if (verification.isExpired()) {
            throw new AuthException(HttpStatus.GONE, "인증번호 만료 (3분 초과)");
        }

        if (!verification.getVerificationCode().equals(request.getCode())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "인증번호 불일치");
        }

        verification.setIsVerified(true);
        return Map.of("verified", true);
    }
}
