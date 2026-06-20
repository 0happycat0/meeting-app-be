package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.configuration.LiveKitConfig;
import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.entity.User;
import com.happycat.meetingappbe.enums.ParticipantRole;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import io.livekit.server.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.io.IOException;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LiveKitAccessService {
    LiveKitConfig properties;

    public LiveKitTokenIssue createJoinToken(
            Meeting meeting,
            User user,
            ParticipantRole role,
            String tokenIdentifier,
            String requestedDisplayName
    ) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.getTokenTtlSeconds());

        AccessToken token = new AccessToken(properties.getApiKey(), properties.getApiSecret());
        token.setIdentity(user.getId());
        token.setName(displayName(user, requestedDisplayName));
        token.setTtl(properties.getTokenTtlSeconds() * 1000);
        token.setMetadata("""
                {"tokenIdentifier":"%s","role":"%s"}
                """.formatted(tokenIdentifier, role.name()).trim());
        token.addGrants(
                new RoomJoin(true),
                new RoomName(meeting.getRoomId()),
                new CanPublish(true),
                new CanSubscribe(true),
                new CanPublishData(true));

        return LiveKitTokenIssue.builder()
                .liveKitUrl(properties.getWsUrl())
                .token(token.toJwt())
                .roomName(meeting.getRoomId())
                .identity(user.getId())
                .expiresAt(expiresAt)
                .build();
    }

    public void removeParticipant(String roomName, String identity) {
        try {
            RoomServiceClient client = RoomServiceClient.createClient(
                    properties.getApiUrl(),
                    properties.getApiKey(),
                    properties.getApiSecret());
            Response<?> response = client.removeParticipant(roomName, identity).execute();
            if (!response.isSuccessful() && response.code() != 404) {
                throw new AppException(ErrorCode.LIVEKIT_SERVICE_UNAVAILABLE);
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.LIVEKIT_SERVICE_UNAVAILABLE);
        }
    }

    String displayName(User user, String requestedDisplayName) {
        if (requestedDisplayName != null && !requestedDisplayName.trim().isBlank()) {
            return requestedDisplayName.trim();
        }
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getId();
    }
}
