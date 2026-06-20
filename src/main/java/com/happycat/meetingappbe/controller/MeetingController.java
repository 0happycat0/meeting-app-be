package com.happycat.meetingappbe.controller;

import com.happycat.meetingappbe.dto.ApiResponse;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.MeetingCreationRequest;
import com.happycat.meetingappbe.dto.request.MeetingUpdateRequest;
import com.happycat.meetingappbe.dto.response.JoinMeetingResponse;
import com.happycat.meetingappbe.dto.response.MeetingResponse;
import com.happycat.meetingappbe.enums.MeetingStatus;
import com.happycat.meetingappbe.enums.MeetingType;
import com.happycat.meetingappbe.service.MeetingService;
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
@RequestMapping("/meetings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingController {
    private static final String USER_OR_ADMIN =
            "hasAnyAuthority('PERM_client_user', 'PERM_client_admin', 'ROLE_USER', 'ROLE_ADMIN')";

    MeetingService meetingService;

    @PostMapping
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingResponse> createMeeting(
            @RequestBody @Valid MeetingCreationRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingResponse>builder()
                .message("Create meeting successfully")
                .result(meetingService.createMeeting(request, subject(authentication)))
                .build();
    }

    @GetMapping("/{meetingId}")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingResponse> getMeeting(@PathVariable String meetingId, JwtAuthenticationToken authentication) {
        return ApiResponse.<MeetingResponse>builder()
                .result(meetingService.getMeeting(meetingId, subject(authentication), isAdmin(authentication)))
                .build();
    }

    @GetMapping("/my")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingResponse>> getMyMeetings(
            @RequestParam(required = false) MeetingStatus status,
            @RequestParam(required = false) MeetingType type,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<PageResponse<MeetingResponse>>builder()
                .result(meetingService.getMyMeetings(subject(authentication), status, type))
                .build();
    }

    @PutMapping("/{meetingId}")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingResponse> updateMeeting(
            @PathVariable String meetingId,
            @RequestBody @Valid MeetingUpdateRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingResponse>builder()
                .message("Update meeting successfully")
                .result(meetingService.updateMeeting(meetingId, request, subject(authentication), isAdmin(authentication)))
                .build();
    }

    @PatchMapping("/{meetingId}/cancel")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingResponse> cancelMeeting(@PathVariable String meetingId, JwtAuthenticationToken authentication) {
        return ApiResponse.<MeetingResponse>builder()
                .message("Cancel meeting successfully")
                .result(meetingService.cancelMeeting(meetingId, subject(authentication), isAdmin(authentication)))
                .build();
    }

    @PatchMapping("/{meetingId}/end")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingResponse> endMeeting(@PathVariable String meetingId, JwtAuthenticationToken authentication) {
        return ApiResponse.<MeetingResponse>builder()
                .message("End meeting successfully")
                .result(meetingService.endMeeting(meetingId, subject(authentication), isAdmin(authentication)))
                .build();
    }

    @GetMapping("/join/{joinCode}")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<JoinMeetingResponse> findByJoinCode(
            @PathVariable String joinCode,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<JoinMeetingResponse>builder()
                .result(meetingService.findByJoinCode(joinCode, subject(authentication), isAdmin(authentication)))
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
