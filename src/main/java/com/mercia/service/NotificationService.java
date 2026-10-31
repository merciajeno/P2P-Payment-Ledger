package com.mercia.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mercia.model.Notification;
import com.mercia.model.User;
import com.mercia.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepo;

    // REQUIRES_NEW — same reason as FraudAlertSaveService.
    // Notifications must survive even if the calling transaction rolls back.
    // e.g. a blocked transaction still needs to notify the sender.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(User user, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notificationRepo.save(notification);
        log.info("Notification sent to {}: {}", user.getEmail(), message);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAll(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepo.markAllAsReadByUserId(userId);
    }
}

