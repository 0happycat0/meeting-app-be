package com.happycat.meetingappbe.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LiveKitJoinTokenRequest {
    @Size(max = 100, message = "LIVEKIT_DISPLAY_NAME_INVALID")
    String name;
}
