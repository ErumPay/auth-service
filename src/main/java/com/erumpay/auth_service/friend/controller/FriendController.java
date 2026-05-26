package com.erumpay.auth_service.friend.controller;

import com.erumpay.auth_service.friend.dto.FavoriteRequest;
import com.erumpay.auth_service.friend.dto.FriendRequestDto;
import com.erumpay.auth_service.friend.service.FriendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // 14. 친구 목록 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> getFriends(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Boolean favorite) {
        List<Map<String, Object>> friends = friendService.getFriends(userId, favorite);
        return ResponseEntity.ok(Map.of("friends", friends));
    }

    // 15. 받은 친구 요청 목록 조회
    @GetMapping("/requests/received")
    public ResponseEntity<Map<String, Object>> getReceivedRequests(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(Map.of("requests", friendService.getReceivedRequests(userId)));
    }

    // 16. 친구 요청 수락
    @PostMapping("/requests/{relationId}/accept")
    public ResponseEntity<Map<String, String>> acceptRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long relationId) {
        return ResponseEntity.ok(friendService.acceptRequest(userId, relationId));
    }

    // 17. 친구 요청 거절
    @PostMapping("/requests/{relationId}/reject")
    public ResponseEntity<Map<String, String>> rejectRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long relationId) {
        return ResponseEntity.ok(friendService.rejectRequest(userId, relationId));
    }

    // 18. 친구 삭제
    @DeleteMapping("/{friendUserId}")
    public ResponseEntity<Map<String, String>> deleteFriend(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendUserId) {
        return ResponseEntity.ok(friendService.deleteFriend(userId, friendUserId));
    }

    // 19. 즐겨찾기 토글
    @PatchMapping("/{friendUserId}/favorite")
    public ResponseEntity<Map<String, Boolean>> toggleFavorite(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long friendUserId,
            @Valid @RequestBody FavoriteRequest request) {
        return ResponseEntity.ok(friendService.toggleFavorite(userId, friendUserId, request.getIsFavorite()));
    }

    // 20. 친구 초대 링크 생성
    @PostMapping("/invite/link")
    public ResponseEntity<Map<String, Object>> createInviteLink(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(friendService.createInviteLink(userId));
    }

    // 21. 초대 링크로 친구 추가
    @PostMapping("/invite/{token}")
    public ResponseEntity<Map<String, Object>> acceptInviteLink(
            @AuthenticationPrincipal Long userId,
            @PathVariable String token) {
        return ResponseEntity.ok(friendService.acceptInviteLink(userId, token));
    }

    // 23. 친구 요청 보내기
    @PostMapping("/requests")
    public ResponseEntity<Map<String, Object>> sendFriendRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FriendRequestDto request) {
        return ResponseEntity.ok(friendService.sendFriendRequest(userId, request.getFriendUserId()));
    }
}
