package com.happycat.meetingappbe.entity;

import com.happycat.meetingappbe.enums.TokenIssueStatus;
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
@Table(name = "meeting_token_logs",
        indexes = {
                @Index(name = "idx_token_log_meeting_id", columnList = "meeting_id"),
                @Index(name = "idx_token_log_user_id", columnList = "user_id"),
                @Index(name = "idx_token_log_token_identifier", columnList = "token_identifier")
        })
public class MeetingTokenLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "livekit_room_id", nullable = false)
    String liveKitRoomId;

    @Column(name = "token_identifier", nullable = false)
    String tokenIdentifier;

    @Column(name = "issued_at", nullable = false, updatable = false)
    Instant issuedAt;

    @Column(name = "expires_at")
    Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_status", nullable = false)
    TokenIssueStatus issueStatus;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    String failureReason;

    @PrePersist
    void onCreate() {
        if (issuedAt == null) {
            issuedAt = Instant.now();
        }
    }
}
