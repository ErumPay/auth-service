package com.erumpay.auth_service.friend.repository;

import com.erumpay.auth_service.friend.entity.FriendRelation;
import com.erumpay.auth_service.friend.entity.FriendRelation.FriendStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRelationRepository extends JpaRepository<FriendRelation, Long> {

    // 내 친구 목록 (ACCEPTED)
    List<FriendRelation> findByUser_UserIdAndStatus(Long userId, FriendStatus status);

    // 즐겨찾기 친구만
    List<FriendRelation> findByUser_UserIdAndStatusAndIsFavoriteTrue(Long userId, FriendStatus status);

    // 받은 친구 요청 (PENDING, 내가 friend_user_id)
    List<FriendRelation> findByFriend_UserIdAndStatus(Long friendUserId, FriendStatus status);

    // 두 사용자 간 관계 조회
    Optional<FriendRelation> findByUser_UserIdAndFriend_UserId(Long userId, Long friendUserId);

    // 친구 관계 존재 여부 (ACCEPTED)
    boolean existsByUser_UserIdAndFriend_UserIdAndStatus(Long userId, Long friendUserId, FriendStatus status);

    // 탈퇴 시 본인 관련 관계 전부 조회
    List<FriendRelation> findByUser_UserIdOrFriend_UserId(Long userId, Long friendUserId);
}
