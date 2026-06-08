package com.happycat.meetingappbe.controller;

import com.happycat.meetingappbe.dto.ApiResponse;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.MeetingInvitationCreationRequest;
import com.happycat.meetingappbe.dto.response.MeetingInvitationResponse;
import com.happycat.meetingappbe.service.MeetingInvitationService;
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
public class MeetingInvitationController {
    private static final String USER_OR_ADMIN =
            "hasAnyAuthority('PERM_client_user', 'PERM_client_admin', 'ROLE_USER', 'ROLE_ADMIN')";

    MeetingInvitationService invitationService;

    @PostMapping("/meetings/{meetingId}/invitations")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingInvitationResponse> createInvitation(
            @PathVariable String meetingId,
            @RequestBody @Valid MeetingInvitationCreationRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingInvitationResponse>builder()
                .message("Create invitation successfully")
                .result(invitationService.createInvitation(
                        meetingId,
                        request,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @GetMapping("/meetings/{meetingId}/invitations")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingInvitationResponse>> getMeetingInvitations(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<PageResponse<MeetingInvitationResponse>>builder()
                .result(invitationService.getMeetingInvitations(
                        meetingId,
                        subject(authentication),
                        isAdmin(authentication)))
                .build();
    }

    @GetMapping("/invitations/my")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingInvitationResponse>> getMyInvitations(JwtAuthenticationToken authentication) {
        return ApiResponse.<PageResponse<MeetingInvitationResponse>>builder()
                .result(invitationService.getMyInvitations(subject(authentication)))
                .build();
    }

    @PatchMapping("/invitations/{invitationId}/accept")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingInvitationResponse> acceptInvitation(
            @PathVariable String invitationId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingInvitationResponse>builder()
                .message("Accept invitation successfully")
                .result(invitationService.acceptInvitation(invitationId, subject(authentication)))
                .build();
    }

    @PatchMapping("/invitations/{invitationId}/decline")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingInvitationResponse> declineInvitation(
            @PathVariable String invitationId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingInvitationResponse>builder()
                .message("Decline invitation successfully")
                .result(invitationService.declineInvitation(invitationId, subject(authentication)))
                .build();
    }

    @PatchMapping("/invitations/{invitationId}/cancel")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<MeetingInvitationResponse> cancelInvitation(
            @PathVariable String invitationId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<MeetingInvitationResponse>builder()
                .message("Cancel invitation successfully")
                .result(invitationService.cancelInvitation(
                        invitationId,
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
