package com.happycat.meetingappbe.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    // 10xx: system error
    // 20xx: auth error
    // 30xx: user error
    // 40xx: meeting error
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2002, "You do not have permission", HttpStatus.FORBIDDEN),

    USER_EXISTED(3001, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_EXISTED(3002, "User not existed", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(3003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(3004, "Password must be at least 6 characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(3005, "Email format is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(3006, "Email already exists", HttpStatus.CONFLICT),

    MEETING_NOT_FOUND(4001, "Meeting not found", HttpStatus.NOT_FOUND),
    MEETING_TITLE_INVALID(4002, "Meeting title is invalid", HttpStatus.BAD_REQUEST),
    MEETING_DESCRIPTION_INVALID(4003, "Meeting description is invalid", HttpStatus.BAD_REQUEST),
    MEETING_TYPE_INVALID(4004, "Meeting type is required", HttpStatus.BAD_REQUEST),
    MEETING_TIME_INVALID(4005, "Meeting schedule time is invalid", HttpStatus.BAD_REQUEST),
    MEETING_ACCESS_DENIED(4006, "You do not have access to this meeting", HttpStatus.FORBIDDEN),
    MEETING_STATE_INVALID(4007, "Meeting status does not allow this action", HttpStatus.CONFLICT),
    JOIN_CODE_INVALID(4008, "Join code is invalid", HttpStatus.BAD_REQUEST),
    JOIN_CODE_GENERATION_FAILED(4009, "Unable to generate a unique join code", HttpStatus.INTERNAL_SERVER_ERROR),

    INVITATION_NOT_FOUND(4101, "Invitation not found", HttpStatus.NOT_FOUND),
    INVITATION_DUPLICATED(4102, "Invitation already exists for this user", HttpStatus.CONFLICT),
    INVITATION_ACCESS_DENIED(4103, "You do not have access to this invitation", HttpStatus.FORBIDDEN),
    INVITATION_STATE_INVALID(4104, "Invitation status does not allow this action", HttpStatus.CONFLICT),
    INVITATION_INVITEE_INVALID(4105, "Invitation invitee is invalid", HttpStatus.BAD_REQUEST),

    PARTICIPANT_NOT_FOUND(4201, "Participant not found", HttpStatus.NOT_FOUND),
    PARTICIPANT_ACCESS_DENIED(4202, "You do not have access to this participant", HttpStatus.FORBIDDEN),
    PARTICIPANT_STATE_INVALID(4203, "Participant status does not allow this action", HttpStatus.CONFLICT),

    LIVEKIT_SERVICE_UNAVAILABLE(4301, "LiveKit service unavailable", HttpStatus.BAD_GATEWAY),
    LIVEKIT_DISPLAY_NAME_INVALID(4302, "LiveKit display name is invalid", HttpStatus.BAD_REQUEST),

    TRANSCRIPT_SEGMENT_INVALID(4401, "Transcript segment is invalid", HttpStatus.BAD_REQUEST),

    MEETING_MINUTES_NOT_FOUND(4501, "Meeting minutes not found", HttpStatus.NOT_FOUND),
    MEETING_MINUTES_NO_TRANSCRIPT(4502, "Meeting has no transcript content", HttpStatus.CONFLICT),
    MEETING_MINUTES_GENERATION_FAILED(4503, "Meeting minutes generation failed", HttpStatus.BAD_GATEWAY),
    MEETING_MINUTES_STATE_INVALID(4504, "Meeting minutes status does not allow this action", HttpStatus.CONFLICT),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    int code;
    String message;
    HttpStatusCode statusCode;

}
