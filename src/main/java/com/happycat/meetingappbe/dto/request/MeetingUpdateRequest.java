package com.happycat.meetingappbe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeetingUpdateRequest {
    @NotBlank(message = "MEETING_TITLE_INVALID")
    @Size(max = 255, message = "MEETING_TITLE_INVALID")
    String title;

    @Size(max = 5000, message = "MEETING_DESCRIPTION_INVALID")
    String description;

    Instant scheduledStartAt;

    Instant scheduledEndAt;
}
