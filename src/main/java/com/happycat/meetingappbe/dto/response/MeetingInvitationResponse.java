package com.happycat.meetingappbe.dto.response;

import com.happycat.meetingappbe.enums.InvitationStatus;
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
public class MeetingInvitationResponse {
    String id;
    String meetingId;
    String meetingTitle;
    MeetingType meetingType;
    MeetingStatus meetingStatus;
    Instant scheduledStartAt;
    Instant scheduledEndAt;
    String inviterId;
    String inviterFirstName;
    String inviterLastName;
    String inviteeId;
    InvitationStatus status;
    Instant sentAt;
    Instant respondedAt;
}
