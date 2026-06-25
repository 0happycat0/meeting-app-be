package com.happycat.meetingappbe.controller;

import com.happycat.meetingappbe.dto.ApiResponse;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.response.MeetingMinutesListItemResponse;
import com.happycat.meetingappbe.dto.response.MeetingMinutesResponse;
import com.happycat.meetingappbe.service.MeetingMinutesService;
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
public class MeetingMinutesController {
    private static final String USER_OR_ADMIN =
            "hasAnyAuthority('PERM_client_user', 'PERM_client_admin', 'ROLE_USER', 'ROLE_ADMIN')";

    MeetingMinutesService meetingMinutesService;

    @GetMapping("/meetings/my/minutes")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingMinutesListItemResponse>> getMyMinutes(JwtAuthenticationToken authentication) {
        return ApiResponse.<PageResponse<MeetingMinutesListItemResponse>>builder()
                .result(meetingMinutesService.getMyMinutes(subject(authentication), isAdmin(authentication)))
                .build();
    }

    @PostMapping("/meetings/{meetingId}/minutes/generate")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingMinutesResponse> generateMinutes(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingMinutesResponse>builder()
                .message("Generate meeting minutes started")
                .result(meetingMinutesService.generateMinutes(
                        meetingId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @GetMapping("/meetings/{meetingId}/minutes")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingMinutesResponse> getMinutes(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingMinutesResponse>builder()
                .result(meetingMinutesService.getMinutes(
                        meetingId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @PatchMapping("/meetings/{meetingId}/minutes/publish")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingMinutesResponse> publishMinutes(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingMinutesResponse>builder()
                .message("Publish meeting minutes successfully")
                .result(meetingMinutesService.publishMinutes(
                        meetingId,
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
