package com.happycat.meetingappbe.entity;

import com.happycat.meetingappbe.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "meeting_invitations",
        indexes = {
                @Index(name = "idx_invitation_meeting_status", columnList = "meeting_id,status"),
                @Index(name = "idx_invitation_invitee_status", columnList = "invitee_id,status")
        })
public class MeetingInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    InvitationStatus status;

    @Column(name = "sent_at", nullable = false, updatable = false)
    Instant sentAt;

    @Column(name = "responded_at")
    Instant respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (sentAt == null) {
            sentAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
