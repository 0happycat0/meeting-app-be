package com.happycat.meetingappbe.entity;

import com.happycat.meetingappbe.enums.MeetingMinutesStatus;
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
@Table(name = "meeting_minutes",
        indexes = {
                @Index(name = "idx_minutes_meeting_id", columnList = "meeting_id"),
                @Index(name = "idx_minutes_status", columnList = "status")
        })
public class MeetingMinutes {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    Meeting meeting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MeetingMinutesStatus status;

    @Column(name = "content_markdown", columnDefinition = "TEXT")
    String contentMarkdown;

    @Column(nullable = false)
    boolean published;

    @Column
    String model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    User generatedBy;

    @Column(name = "generated_at")
    Instant generatedAt;

    @Column(name = "source_segment_count", nullable = false)
    int sourceSegmentCount;

    @Column(name = "chunk_count", nullable = false)
    int chunkCount;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    String failureReason;

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
