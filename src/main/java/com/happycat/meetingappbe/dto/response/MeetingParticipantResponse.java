package com.happycat.meetingappbe.dto.response;

import com.happycat.meetingappbe.enums.JoinSource;
import com.happycat.meetingappbe.enums.ParticipantRole;
import com.happycat.meetingappbe.enums.ParticipationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeetingParticipantResponse {
    String id;
    String meetingId;
    String userId;
    String username;
    String firstName;
    String lastName;
    String email;
    ParticipantRole role;
    JoinSource joinSource;
    ParticipationStatus participationStatus;
    Instant requestedAt;
    Instant approvedAt;
    String approvedById;
    Instant joinedAt;
    Instant leftAt;
    Instant removedAt;
    String removedById;
}
