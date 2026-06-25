package com.happycat.meetingappbe.dto.response;

import com.happycat.meetingappbe.enums.MeetingMinutesListStatus;
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
public class MeetingMinutesListItemResponse {
    String meetingId;
    String meetingTitle;
    String meetingDescription;
    MeetingType meetingType;
    String hostId;
    String hostFirstName;
    String hostLastName;
    MeetingStatus meetingStatus;
    Instant scheduledStartAt;
    Instant scheduledEndAt;
    String minutesId;
    MeetingMinutesListStatus minutesStatus;
    boolean published;
    Instant generatedAt;
    Instant updatedAt;
}
