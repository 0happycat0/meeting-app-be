package com.happycat.meetingappbe.dto.request;

import com.happycat.meetingappbe.enums.MeetingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeetingCreationRequest {
    @NotBlank(message = "MEETING_TITLE_INVALID")
    @Size(max = 255, message = "MEETING_TITLE_INVALID")
    String title;

    @Size(max = 5000, message = "MEETING_DESCRIPTION_INVALID")
    String description;

    @NotNull(message = "MEETING_TYPE_INVALID")
    MeetingType meetingType;

    Instant scheduledStartAt;

    Instant scheduledEndAt;
}
