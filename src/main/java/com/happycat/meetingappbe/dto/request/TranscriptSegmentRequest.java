package com.happycat.meetingappbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TranscriptSegmentRequest {
    @NotBlank(message = "TRANSCRIPT_SEGMENT_INVALID")
    String segmentId;

    @NotBlank(message = "TRANSCRIPT_SEGMENT_INVALID")
    @Size(max = 10000, message = "TRANSCRIPT_SEGMENT_INVALID")
    String text;

    @PositiveOrZero(message = "TRANSCRIPT_SEGMENT_INVALID")
    Double latencyMsFromFirstAudio;

    @PositiveOrZero(message = "TRANSCRIPT_SEGMENT_INVALID")
    Integer tokenCount;

    @PositiveOrZero(message = "TRANSCRIPT_SEGMENT_INVALID")
    Integer totalTokensEmitted;

    Instant clientCreatedAt;
}
