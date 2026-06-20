package com.happycat.meetingappbe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TranscriptSegmentBatchRequest {
    @Valid
    @NotEmpty(message = "TRANSCRIPT_SEGMENT_INVALID")
    @Size(max = 100, message = "TRANSCRIPT_SEGMENT_INVALID")
    List<TranscriptSegmentRequest> segments;
}
