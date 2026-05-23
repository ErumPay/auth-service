package com.erumpay.auth_service.invite.repository;

import com.erumpay.auth_service.invite.entity.FriendAddLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface FriendAddLinkRepository extends JpaRepository<FriendAddLink, Long> {

    Optional<FriendAddLink> findByInviteToken(String inviteToken);

    @Modifying
    @Query("UPDATE FriendAddLink l SET l.isUsed = true WHERE l.inviter.userId = :userId AND l.isUsed = false")
    int markAllUsedByInviterId(Long userId);
}
