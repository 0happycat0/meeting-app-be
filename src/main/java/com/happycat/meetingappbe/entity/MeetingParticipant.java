package com.happycat.meetingappbe.entity;

import com.happycat.meetingappbe.enums.JoinSource;
import com.happycat.meetingappbe.enums.ParticipantRole;
import com.happycat.meetingappbe.enums.ParticipationStatus;
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
@Table(name = "meeting_participants",
        uniqueConstraints = @UniqueConstraint(name = "uk_meeting_participant_user",
                columnNames = {"meeting_id", "user_id"}),
        indexes = {
                @Index(name = "idx_participant_meeting_status", columnList = "meeting_id,participation_status"),
                @Index(name = "idx_participant_user_id", columnList = "user_id")
        })
public class MeetingParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "join_source", nullable = false)
    JoinSource joinSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "participation_status", nullable = false)
    ParticipationStatus participationStatus;

    @Column(name = "requested_at")
    Instant requestedAt;

    @Column(name = "approved_at")
    Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    User approvedBy;

    @Column(name = "joined_at")
    Instant joinedAt;

    @Column(name = "left_at")
    Instant leftAt;

    @Column(name = "removed_at")
    Instant removedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "removed_by")
    User removedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
