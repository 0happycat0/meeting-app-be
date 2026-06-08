package com.happycat.meetingappbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeetingInvitationCreationRequest {
    @NotBlank(message = "INVITATION_INVITEE_INVALID")
    String inviteeId;
}
