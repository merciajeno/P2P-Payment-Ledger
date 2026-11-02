package com.mercia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mercia.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Recent audit entries — for admin view, newest first
    List<AuditLog> findTop50ByOrderByPerformedAtDesc();

    // All actions by a specific admin
    List<AuditLog> findByAdminIdOrderByPerformedAtDesc(Long adminId);
}
