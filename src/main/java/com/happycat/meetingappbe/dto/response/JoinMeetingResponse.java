package com.happycat.meetingappbe.dto.response;

import com.happycat.meetingappbe.enums.MeetingStatus;
import com.happycat.meetingappbe.enums.MeetingType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JoinMeetingResponse {
    String id;
    String title;
    String description;
    String hostId;
    String hostFirstName;
    String hostLastName;
    MeetingType meetingType;
    String joinCode;
    Instant scheduledStartAt;
    Instant scheduledEndAt;
    MeetingStatus status;
}
