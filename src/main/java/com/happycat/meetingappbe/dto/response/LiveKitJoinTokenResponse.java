package com.happycat.meetingappbe.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LiveKitJoinTokenResponse {
    String liveKitUrl;
    String token;
    String roomName;
    String identity;
    Instant expiresAt;
}
