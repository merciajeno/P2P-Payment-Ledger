package com.mercia.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mercia.dto.NotificationResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;
	private final UserRepository userRepository;

	// All notifications for the logged-in user
	@GetMapping
	public ResponseEntity<List<NotificationResponse>> getAll(Authentication authentication) {
		User user = getUser(authentication);
		List<NotificationResponse> response = notificationService.getAll(user.getId()).stream()
				.map(n -> new NotificationResponse(n.getId(), n.getMessage(), n.isRead(), n.getCreatedAt())).toList();
		return ResponseEntity.ok(response);
	}

	// Unread count — for the bell icon badge
	@GetMapping("/unread-count")
	public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
		User user = getUser(authentication);
		return ResponseEntity.ok(notificationService.getUnreadCount(user.getId()));
	}

	// Mark all as read
	@PostMapping("/mark-read")
	public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
		User user = getUser(authentication);
		notificationService.markAllAsRead(user.getId());
		return ResponseEntity.ok().build();
	}

	private User getUser(Authentication authentication) {
		return userRepository.findByName(authentication.getName())
				.orElseThrow(() -> new UserNotFoundException("User not found"));
	}
}