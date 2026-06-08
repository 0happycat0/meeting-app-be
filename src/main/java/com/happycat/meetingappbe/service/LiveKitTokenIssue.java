package com.happycat.meetingappbe.service;

import lombok.Builder;

import java.time.Instant;

@Builder
public record LiveKitTokenIssue(
        String liveKitUrl,
        String token,
        String roomName,
        String identity,
        Instant expiresAt
) {
}
