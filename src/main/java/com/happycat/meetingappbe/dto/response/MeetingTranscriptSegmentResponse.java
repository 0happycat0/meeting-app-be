package com.happycat.meetingappbe.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeetingTranscriptSegmentResponse {
    String id;
    String meetingId;
    String participantId;
    String userId;
    String username;
    String firstName;
    String lastName;
    String segmentId;
    String text;
    Double latencyMsFromFirstAudio;
    Integer tokenCount;
    Integer totalTokensEmitted;
    Instant clientCreatedAt;
    Instant createdAt;
}
