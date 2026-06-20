package com.happycat.meetingappbe.entity;

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
@Table(name = "meeting_transcript_segments",
        uniqueConstraints = @UniqueConstraint(name = "uk_transcript_segment_dedupe",
                columnNames = {"meeting_id", "participant_id", "segment_id"}),
        indexes = {
                @Index(name = "idx_transcript_meeting_created", columnList = "meeting_id,created_at"),
                @Index(name = "idx_transcript_participant", columnList = "participant_id"),
                @Index(name = "idx_transcript_user", columnList = "user_id")
        })
public class MeetingTranscriptSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    MeetingParticipant participant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "segment_id", nullable = false)
    String segmentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    String text;

    @Column(name = "latency_ms_from_first_audio")
    Double latencyMsFromFirstAudio;

    @Column(name = "token_count")
    Integer tokenCount;

    @Column(name = "total_tokens_emitted")
    Integer totalTokensEmitted;

    @Column(name = "client_created_at")
    Instant clientCreatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
    }
}
