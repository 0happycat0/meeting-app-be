package com.happycat.meetingappbe.entity;

import com.happycat.meetingappbe.enums.MeetingStatus;
import com.happycat.meetingappbe.enums.MeetingType;
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
@Table(name = "meetings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meeting_room_id", columnNames = "room_id"),
                @UniqueConstraint(name = "uk_meeting_join_code", columnNames = "join_code")
        },
        indexes = {
                @Index(name = "idx_meeting_host_id", columnList = "host_id"),
                @Index(name = "idx_meeting_status", columnList = "status")
        })
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    User host;

    @Enumerated(EnumType.STRING)
    @Column(name = "meeting_type", nullable = false)
    MeetingType meetingType;

    @Column(name = "room_id", nullable = false)
    String roomId;

    @Column(name = "join_code", nullable = false, length = 8)
    String joinCode;

    @Column(name = "scheduled_start_at")
    Instant scheduledStartAt;

    @Column(name = "scheduled_end_at")
    Instant scheduledEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MeetingStatus status;

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
