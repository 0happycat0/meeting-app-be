package com.happycat.meetingappbe.dto.response;

import com.happycat.meetingappbe.enums.MeetingMinutesStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeetingMinutesResponse {
    String id;
    String meetingId;
    MeetingMinutesStatus status;
    String contentMarkdown;
    boolean published;
    String model;
    String generatedById;
    Instant generatedAt;
    Instant updatedAt;
    int sourceSegmentCount;
    int chunkCount;
    String failureReason;
}
