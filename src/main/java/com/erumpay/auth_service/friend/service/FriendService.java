package com.erumpay.auth_service.friend.service;

import com.erumpay.auth_service.auth.entity.AuthUser;
import com.erumpay.auth_service.auth.entity.AuthUser.UserStatus;
import com.erumpay.auth_service.auth.repository.AuthUserRepository;
import com.erumpay.auth_service.common.exception.AuthException;
import com.erumpay.auth_service.common.kafka.AuthEventProducer;
import com.erumpay.auth_service.common.util.AesEncryptionUtil;
import com.erumpay.auth_service.friend.entity.FriendRelation;
import com.erumpay.auth_service.friend.entity.FriendRelation.FriendStatus;
import com.erumpay.auth_service.friend.repository.FriendRelationRepository;
import com.erumpay.auth_service.invite.entity.FriendAddLink;
import com.erumpay.auth_service.invite.repository.FriendAddLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRelationRepository friendRelationRepository;
    private final FriendAddLinkRepository friendAddLinkRepository;
    private final AuthUserRepository userRepository;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final AuthEventProducer authEventProducer;

    // 14. 친구 목록 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFriends(Long userId, Boolean favorite) {
        List<FriendRelation> relations;
        if (Boolean.TRUE.equals(favorite)) {
            relations = friendRelationRepository.findByUser_UserIdAndStatusAndIsFavoriteTrue(userId, FriendStatus.ACCEPTED);
        } else {
            relations = friendRelationRepository.findByUser_UserIdAndStatus(userId, FriendStatus.ACCEPTED);
        }

        return relations.stream().map(r -> {
            AuthUser friend = r.getFriend();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("relationId", r.getRelationId());
            map.put("userId", friend.getUserId());
            map.put("name", friend.getName() != null ? friend.getName() : "");
            map.put("phoneLastFour", getPhoneLastFour(friend));
            map.put("isFavorite", r.getIsFavorite());
            return map;
        }).toList();
    }

    // 15. 받은 친구 요청 목록 조회
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReceivedRequests(Long userId) {
        List<FriendRelation> requests = friendRelationRepository.findByFriend_UserIdAndStatus(userId, FriendStatus.PENDING);

        return requests.stream().map(r -> {
            AuthUser from = r.getUser();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("relationId", r.getRelationId());
            map.put("fromUserId", from.getUserId());
            map.put("name", from.getName() != null ? from.getName() : "");
            map.put("phoneLastFour", getPhoneLastFour(from));
            map.put("createdAt", r.getCreatedAt().toString());
            return map;
        }).toList();
    }

    // 16. 친구 요청 수락
    @Transactional
    public Map<String, String> acceptRequest(Long userId, Long relationId) {
        FriendRelation relation = friendRelationRepository.findById(relationId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없음"));

        if (!relation.getFriend().getUserId().equals(userId)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "본인에게 온 요청이 아님");
        }
        if (relation.getStatus() == FriendStatus.ACCEPTED) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 친구 관계입니다");
        }

        relation.setStatus(FriendStatus.ACCEPTED);

        // 양방향 레코드 처리: 기존 레코드 있으면 UPDATE, 없으면 INSERT
        Optional<FriendRelation> existingReverse = friendRelationRepository
                .findByUser_UserIdAndFriend_UserId(relation.getFriend().getUserId(), relation.getUser().getUserId());
        if (existingReverse.isPresent()) {
            existingReverse.get().setStatus(FriendStatus.ACCEPTED);
        } else {
            FriendRelation reverse = FriendRelation.builder()
                    .user(relation.getFriend())
                    .friend(relation.getUser())
                    .status(FriendStatus.ACCEPTED)
                    .isFavorite(false)
                    .build();
            friendRelationRepository.save(reverse);
        }

        return Map.of("message", "친구 추가 완료");
    }

    // 17. 친구 요청 거절
    @Transactional
    public Map<String, String> rejectRequest(Long userId, Long relationId) {
        FriendRelation relation = friendRelationRepository.findById(relationId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "친구 요청을 찾을 수 없음"));

        if (!relation.getFriend().getUserId().equals(userId)) {
            throw new AuthException(HttpStatus.FORBIDDEN, "본인에게 온 요청이 아님");
        }
        if (relation.getStatus() != FriendStatus.PENDING) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 처리된 요청");
        }

        relation.setStatus(FriendStatus.DELETED);
        return Map.of("message", "요청 거절 완료");
    }

    // 18. 친구 삭제
    @Transactional
    public Map<String, String> deleteFriend(Long userId, Long friendUserId) {
        Optional<FriendRelation> rel1 = friendRelationRepository.findByUser_UserIdAndFriend_UserId(userId, friendUserId);
        Optional<FriendRelation> rel2 = friendRelationRepository.findByUser_UserIdAndFriend_UserId(friendUserId, userId);

        if (rel1.isEmpty() && rel2.isEmpty()) {
            throw new AuthException(HttpStatus.NOT_FOUND, "친구 관계를 찾을 수 없음");
        }

        rel1.ifPresent(r -> {
            if (r.getStatus() == FriendStatus.DELETED) {
                throw new AuthException(HttpStatus.CONFLICT, "이미 삭제된 친구");
            }
            r.setStatus(FriendStatus.DELETED);
        });
        rel2.ifPresent(r -> r.setStatus(FriendStatus.DELETED));

        return Map.of("message", "친구 삭제 완료");
    }

    // 19. 즐겨찾기 토글
    @Transactional
    public Map<String, Boolean> toggleFavorite(Long userId, Long friendUserId, Boolean isFavorite) {
        FriendRelation relation = friendRelationRepository.findByUser_UserIdAndFriend_UserId(userId, friendUserId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "친구 관계를 찾을 수 없음"));

        if (relation.getStatus() != FriendStatus.ACCEPTED) {
            throw new AuthException(HttpStatus.CONFLICT, "ACCEPTED 상태가 아닌 친구에게는 즐겨찾기 설정 불가");
        }

        relation.setIsFavorite(isFavorite);
        return Map.of("isFavorite", isFavorite);
    }

    // 20. 친구 초대 링크 생성
    @Transactional
    public Map<String, Object> createInviteLink(Long userId) {
        AuthUser inviter = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(3);

        FriendAddLink link = FriendAddLink.builder()
                .inviter(inviter)
                .inviteToken(token)
                .expiresAt(expiresAt)
                .isUsed(false)
                .build();
        friendAddLinkRepository.save(link);

        return Map.of(
                "inviteToken", token,
                "inviteUrl", "erumpay://friends/invite/" + token,
                "expiresAt", expiresAt.toString()
        );
    }

    // 21. 초대 링크로 친구 추가
    @Transactional
    public Map<String, Object> acceptInviteLink(Long userId, String token) {
        FriendAddLink link = friendAddLinkRepository.findByInviteToken(token)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "존재하지 않는 초대 토큰"));

        if (link.getIsUsed()) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 사용된 초대 링크 (is_used = TRUE)");
        }
        if (link.isExpired()) {
            throw new AuthException(HttpStatus.GONE, "만료된 초대 링크 (3시간 초과)");
        }

        AuthUser inviter = link.getInviter();
        if (inviter.getUserId().equals(userId)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "자기 자신은 초대할 수 없음");
        }
        if (inviter.getStatus() == UserStatus.WITHDRAWN || inviter.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthException(HttpStatus.FORBIDDEN, "초대자가 탈퇴했거나 정지된 회원");
        }

        // 이미 친구인지 확인
        if (friendRelationRepository.existsByUser_UserIdAndFriend_UserIdAndStatus(userId, inviter.getUserId(), FriendStatus.ACCEPTED)) {
            throw new AuthException(HttpStatus.CONFLICT, "이미 친구 관계");
        }

        AuthUser invitee = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));

        // 링크 사용 처리
        link.setIsUsed(true);
        link.setInvitee(invitee);

        // 양방향 즉시 ACCEPTED
        createBidirectionalFriendship(inviter, invitee);

        return Map.of("message", "친구 추가 완료", "friendName", inviter.getName() != null ? inviter.getName() : "");
    }

    // 23. 친구 요청 보내기
    @Transactional
    public Map<String, Object> sendFriendRequest(Long userId, Long friendUserId) {
        if (userId.equals(friendUserId)) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "자기 자신에게 요청 불가");
        }

        AuthUser user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없음"));
        AuthUser friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자"));

        Optional<FriendRelation> existing = friendRelationRepository.findByUser_UserIdAndFriend_UserId(userId, friendUserId);

        if (existing.isPresent()) {
            FriendRelation rel = existing.get();
            if (rel.getStatus() == FriendStatus.ACCEPTED) {
                throw new AuthException(HttpStatus.CONFLICT, "이미 친구 관계 (ACCEPTED 상태)");
            }
            if (rel.getStatus() == FriendStatus.PENDING) {
                throw new AuthException(HttpStatus.CONFLICT, "이미 보낸 요청 (PENDING 상태)");
            }
            // DELETED → PENDING (재요청)
            rel.setStatus(FriendStatus.PENDING);
            authEventProducer.sendFriendRequest(userId, friendUserId);
            return Map.of("relationId", rel.getRelationId(), "message", "친구 요청 완료");
        }

        FriendRelation relation = FriendRelation.builder()
                .user(user)
                .friend(friend)
                .status(FriendStatus.PENDING)
                .isFavorite(false)
                .build();
        friendRelationRepository.save(relation);

        authEventProducer.sendFriendRequest(userId, friendUserId);

        return Map.of("relationId", relation.getRelationId(), "message", "친구 요청 완료");
    }

    // 24. 친구 관계 확인 (내부 API)
    public boolean isFriend(Long userId, Long friendUserId) {
        if (!userRepository.existsById(userId) || !userRepository.existsById(friendUserId)) {
            throw new AuthException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자");
        }

        return friendRelationRepository.existsByUser_UserIdAndFriend_UserIdAndStatus(
                userId, friendUserId, FriendStatus.ACCEPTED);
    }

    private void createBidirectionalFriendship(AuthUser user1, AuthUser user2) {
        // 기존 관계가 있으면 ACCEPTED로 변경, 없으면 새로 생성
        Optional<FriendRelation> rel1 = friendRelationRepository.findByUser_UserIdAndFriend_UserId(user1.getUserId(), user2.getUserId());
        if (rel1.isPresent()) {
            rel1.get().setStatus(FriendStatus.ACCEPTED);
        } else {
            friendRelationRepository.save(FriendRelation.builder()
                    .user(user1).friend(user2).status(FriendStatus.ACCEPTED).isFavorite(false).build());
        }

        Optional<FriendRelation> rel2 = friendRelationRepository.findByUser_UserIdAndFriend_UserId(user2.getUserId(), user1.getUserId());
        if (rel2.isPresent()) {
            rel2.get().setStatus(FriendStatus.ACCEPTED);
        } else {
            friendRelationRepository.save(FriendRelation.builder()
                    .user(user2).friend(user1).status(FriendStatus.ACCEPTED).isFavorite(false).build());
        }
    }

    private String getPhoneLastFour(AuthUser user) {
        if (user.getPhoneNumber() == null) return "";
        try {
            String phone = aesEncryptionUtil.decrypt(user.getPhoneNumber());
            return phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone;
        } catch (Exception e) {
            return "";
        }
    }
}
