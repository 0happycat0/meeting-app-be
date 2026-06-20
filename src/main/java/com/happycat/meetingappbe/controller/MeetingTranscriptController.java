package com.happycat.meetingappbe.controller;

import com.happycat.meetingappbe.dto.ApiResponse;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.TranscriptSegmentBatchRequest;
import com.happycat.meetingappbe.dto.response.MeetingTranscriptSegmentResponse;
import com.happycat.meetingappbe.service.MeetingTranscriptEventService;
import com.happycat.meetingappbe.service.MeetingTranscriptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingTranscriptController {
    private static final String USER_OR_ADMIN =
            "hasAnyAuthority('PERM_client_user', 'PERM_client_admin', 'ROLE_USER', 'ROLE_ADMIN')";

    MeetingTranscriptService transcriptService;
    MeetingTranscriptEventService transcriptEventService;

    @PostMapping("/meetings/{meetingId}/transcript-segments/batch")
    @PreAuthorize(USER_OR_ADMIN)
    ApiResponse<PageResponse<MeetingTranscriptSegmentResponse>> saveTranscriptSegments(
            @PathVariable String meetingId,
            @RequestBody @Valid TranscriptSegmentBatchRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<PageResponse<MeetingTranscriptSegmentResponse>>builder()
                .message("Save transcript segments successfully")
                .result(transcriptService.saveTranscriptSegments(meetingId, subject(authentication), request))
                .build();
    }

    @GetMapping(value = "/meetings/{meetingId}/transcript-events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize(USER_OR_ADMIN)
    SseEmitter subscribeTranscriptEvents(
            @PathVariable String meetingId,
            JwtAuthenticationToken authentication
    ) {
        return transcriptEventService.subscribe(meetingId, subject(authentication));
    }

    private String subject(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return jwt.getSubject();
    }
}
