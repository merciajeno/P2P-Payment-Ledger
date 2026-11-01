package com.mercia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.mercia.dto.NotificationResponse;
import com.mercia.exception.UserNotFoundException;
import com.mercia.model.Notification;
import com.mercia.model.User;
import com.mercia.repository.UserRepository;
import com.mercia.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

	@Mock
	private NotificationService notificationService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private Authentication authentication;

	@InjectMocks
	private NotificationController controller;

	@Test
	void getAllMapsNotificationFieldsForAuthenticatedUser() {
		User user = user(15L);
		Notification notification = new Notification();
		notification.setId(25L);
		notification.setMessage("Transfer completed");
		notification.setRead(false);
		notification.setCreatedAt(Instant.parse("2025-01-01T10:00:00Z"));
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user));
		when(notificationService.getAll(15L)).thenReturn(List.of(notification));

		List<NotificationResponse> response = controller.getAll(authentication).getBody();

		assertEquals(1, response.size());
		assertEquals(25L, response.get(0).id());
		assertEquals("Transfer completed", response.get(0).message());
		assertFalse(response.get(0).read());
		assertEquals(notification.getCreatedAt(), response.get(0).createdAt());
		verify(notificationService).getAll(15L);
	}

	@Test
	void getUnreadCountReturnsCountForAuthenticatedUser() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user(16L)));
		when(notificationService.getUnreadCount(16L)).thenReturn(3L);

		assertEquals(3L, controller.getUnreadCount(authentication).getBody());

		verify(notificationService).getUnreadCount(16L);
	}

	@Test
	void markAllAsReadDelegatesForAuthenticatedUser() {
		when(authentication.getName()).thenReturn("alice");
		when(userRepository.findByName("alice")).thenReturn(Optional.of(user(17L)));

		var response = controller.markAllAsRead(authentication);

		assertEquals(200, response.getStatusCode().value());
		verify(notificationService).markAllAsRead(17L);
	}

	@Test
	void notificationActionsThrowWhenAuthenticatedUserIsMissing() {
		when(authentication.getName()).thenReturn("missing");
		when(userRepository.findByName("missing")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> controller.getAll(authentication));
	}

	private User user(Long id) {
		User user = new User();
		user.setId(id);
		user.setName("alice");
		return user;
	}
}
