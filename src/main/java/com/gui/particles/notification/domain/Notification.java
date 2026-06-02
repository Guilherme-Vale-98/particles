package com.gui.particles.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "recipient_id", nullable = false, updatable = false)
    private UUID recipientId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false)
    private NotificationType type;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Column(name = "secondary_reference_id", updatable = false)
    private UUID secondaryReferenceId;

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    private Notification(
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            UUID referenceId,
            UUID secondaryReferenceId
    ) {
        this.recipientId = Objects.requireNonNull(recipientId, "recipientId must not be null");
        this.actorId = Objects.requireNonNull(actorId, "actorId must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.referenceId = Objects.requireNonNull(referenceId, "referenceId must not be null");
        this.secondaryReferenceId = secondaryReferenceId;
        if (this.recipientId.equals(this.actorId)) {
            throw new IllegalArgumentException("recipientId and actorId must be different");
        }
        this.read = false;
        this.createdAt = Instant.now();
    }

    public static Notification create(
            UUID recipientId,
            UUID actorId,
            NotificationType type,
            UUID referenceId,
            UUID secondaryReferenceId
    ) {
        return new Notification(recipientId, actorId, type, referenceId, secondaryReferenceId);
    }

    public void markRead() {
        if (read) {
            return;
        }
        read = true;
        readAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public UUID recipientId() {
        return recipientId;
    }

    public UUID actorId() {
        return actorId;
    }

    public NotificationType type() {
        return type;
    }

    public UUID referenceId() {
        return referenceId;
    }

    public UUID secondaryReferenceId() {
        return secondaryReferenceId;
    }

    public boolean isRead() {
        return read;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant readAt() {
        return readAt;
    }
}
