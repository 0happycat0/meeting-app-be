package com.happycat.meetingappbe.controller;

import com.happycat.meetingappbe.dto.ApiResponse;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.LiveKitJoinTokenRequest;
import com.happycat.meetingappbe.dto.response.LiveKitJoinTokenResponse;
import com.happycat.meetingappbe.dto.response.MeetingParticipantResponse;
import com.happycat.meetingappbe.service.MeetingParticipantService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingParticipantController {
    private static final String USER_OR_ADMIN =
            "hasAnyAuthority('PERM_client_user', 'PERM_client_admin', 'ROLE_USER', 'ROLE_ADMIN')";

    MeetingParticipantService participantService;

    @PostMapping("/meetings/join/{joinCode}/waiting-room")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> requestWaitingRoomByJoinCode(
            @PathVariable String joinCode,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .message("Request waiting room successfully")
                .result(participantService.requestWaitingRoomByJoinCode(joinCode, subject(authentication)))
                .build();
    }

    @PostMapping("/invitations/{invitationId}/waiting-room")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> requestWaitingRoomByInvitation(
            @PathVariable String invitationId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .message("Request waiting room successfully")
                .result(participantService.requestWaitingRoomByInvitation(invitationId, subject(authentication)))
                .build();
    }

    @GetMapping("/meetings/{meetingId}/waiting-room")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingParticipantResponse>> getWaitingRoom(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<PageResponse<MeetingParticipantResponse>>builder()
                .result(participantService.getWaitingRoom(
                        meetingId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @PatchMapping("/meetings/{meetingId}/waiting-room/{participantId}/approve")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> approveParticipant(
            @PathVariable String meetingId,
            @PathVariable String participantId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .message("Approve participant successfully")
                .result(participantService.approveParticipant(
                        meetingId,
                        participantId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @PatchMapping("/meetings/{meetingId}/waiting-room/{participantId}/reject")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> rejectParticipant(
            @PathVariable String meetingId,
            @PathVariable String participantId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .message("Reject participant successfully")
                .result(participantService.rejectParticipant(
                        meetingId,
                        participantId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @GetMapping("/meetings/{meetingId}/participants")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingParticipantResponse>> getParticipants(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<PageResponse<MeetingParticipantResponse>>builder()
                .result(participantService.getParticipants(
                        meetingId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @GetMapping("/meetings/{meetingId}/my-status")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> getMyStatus(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .result(participantService.getMyStatus(meetingId, subject(authentication)))
                .build();
    }

    @PostMapping("/meetings/{meetingId}/join-token")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<LiveKitJoinTokenResponse> issueJoinToken(
            @PathVariable String meetingId,
            @RequestBody(required = false) @Valid LiveKitJoinTokenRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<LiveKitJoinTokenResponse>builder()
                .message("Issue LiveKit token successfully")
                .result(participantService.issueJoinToken(meetingId, subject(authentication), request))
                .build();
    }

    @PostMapping("/meetings/{meetingId}/leave")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> leaveMeeting(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .message("Leave meeting successfully")
                .result(participantService.leaveMeeting(meetingId, subject(authentication)))
                .build();
    }

    @PostMapping("/meetings/{meetingId}/participants/{participantId}/remove")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingParticipantResponse> removeParticipant(
            @PathVariable String meetingId,
            @PathVariable String participantId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingParticipantResponse>builder()
                .message("Remove participant successfully")
                .result(participantService.removeParticipant(
                        meetingId,
                        participantId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    private String subject(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return jwt.getSubject();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("PERM_client_admin")
                        || authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
