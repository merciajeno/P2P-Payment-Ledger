package com.mercia.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.FriendRequest;
import com.mercia.dto.UserSearchResponse;
import com.mercia.service.FriendService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/friends")
@RequiredArgsConstructor
public class FriendController {

	private final FriendService friendService;

	@GetMapping
	public ResponseEntity<List<UserSearchResponse>> getFriends(Authentication authentication) {
		return ResponseEntity.ok(friendService.getFriends(authentication.getName()));
	}

	@PostMapping
	public ResponseEntity<UserSearchResponse> addFriend(
			@RequestBody FriendRequest request,
			Authentication authentication) {
		if (request == null) {
			throw new IllegalArgumentException("Friend email is required");
		}
		return ResponseEntity.ok(friendService.addFriend(authentication.getName(), request.email()));
	}

	@DeleteMapping
	public ResponseEntity<Void> removeFriend(
			@RequestParam String email,
			Authentication authentication) {
		friendService.removeFriend(authentication.getName(), email);
		return ResponseEntity.noContent().build();
	}
}
