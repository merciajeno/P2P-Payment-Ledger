package com.mercia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mercia.model.Notification;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // All notifications for a user, newest first
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Unread count — for the bell icon
    long countByUserIdAndReadFalse(Long userId);

    // Mark all as read for a user
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
