package com.mercia.model;

import java.time.Instant;

import com.mercia.enums.AdminAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_admin", columnList = "admin_id"),
        @Index(name = "idx_audit_performed_at", columnList = "performed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin who performed the action
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminAction action;

    // The user the action was performed on — nullable
    // e.g. freeze/unfreeze targets a user
    // block/dismiss/settle targets a transaction, so targetUserId is null
    @Column(name = "target_user_id")
    private Long targetUserId;

    // The transaction or alert involved — nullable
    // e.g. block targets a transaction, freeze targets a wallet (no transaction)
    @Column(name = "target_entity_id")
    private Long targetEntityId;

    // Human readable description — e.g. "Froze wallet for user john@example.com"
    @Column(length = 300)
    private String description;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @PrePersist
    protected void onCreate() {
        if (performedAt == null) {
            performedAt = Instant.now();
        }
    }
}
