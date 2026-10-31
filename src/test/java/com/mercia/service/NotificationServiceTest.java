package com.mercia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mercia.model.Notification;
import com.mercia.model.User;
import com.mercia.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationRepository notificationRepo;

	@InjectMocks
	private NotificationService notificationService;

	@Test
	void sendPersistsNotificationForUser() {
		User user = new User();
		user.setEmail("alice@example.com");

		notificationService.send(user, "Transfer completed");

		ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepo).save(captor.capture());
		assertSame(user, captor.getValue().getUser());
		assertEquals("Transfer completed", captor.getValue().getMessage());
	}

	@Test
	void getAllReturnsRepositoryResults() {
		List<Notification> notifications = List.of(new Notification(), new Notification());
		when(notificationRepo.findByUserIdOrderByCreatedAtDesc(23L)).thenReturn(notifications);

		assertSame(notifications, notificationService.getAll(23L));
		verify(notificationRepo).findByUserIdOrderByCreatedAtDesc(23L);
	}

	@Test
	void getUnreadCountReturnsRepositoryCount() {
		when(notificationRepo.countByUserIdAndReadFalse(23L)).thenReturn(4L);

		assertEquals(4L, notificationService.getUnreadCount(23L));
		verify(notificationRepo).countByUserIdAndReadFalse(23L);
	}

	@Test
	void markAllAsReadDelegatesToRepository() {
		notificationService.markAllAsRead(23L);

		verify(notificationRepo).markAllAsReadByUserId(23L);
	}
}
