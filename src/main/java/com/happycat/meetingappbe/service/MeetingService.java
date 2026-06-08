package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.MeetingCreationRequest;
import com.happycat.meetingappbe.dto.request.MeetingUpdateRequest;
import com.happycat.meetingappbe.dto.response.JoinMeetingResponse;
import com.happycat.meetingappbe.dto.response.MeetingResponse;
import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.entity.MeetingParticipant;
import com.happycat.meetingappbe.entity.User;
import com.happycat.meetingappbe.enums.*;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.mapper.MeetingMapper;
import com.happycat.meetingappbe.repository.MeetingInvitationRepository;
import com.happycat.meetingappbe.repository.MeetingParticipantRepository;
import com.happycat.meetingappbe.repository.MeetingRepository;
import com.happycat.meetingappbe.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingService {
    static String JOIN_CODE_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static int JOIN_CODE_LENGTH = 8;
    static int JOIN_CODE_GENERATION_ATTEMPTS = 20;

    MeetingRepository meetingRepository;
    MeetingParticipantRepository participantRepository;
    MeetingInvitationRepository invitationRepository;
    UserRepository userRepository;
    MeetingMapper meetingMapper;
    SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public MeetingResponse createMeeting(MeetingCreationRequest request, String userId) {
        validateSchedule(request.getMeetingType(), request.getScheduledStartAt(), request.getScheduledEndAt());

        User host = findUser(userId);
        Meeting meeting = Meeting.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .host(host)
                .meetingType(request.getMeetingType())
                .roomId("meeting-" + UUID.randomUUID())
                .joinCode(generateJoinCode())
                .scheduledStartAt(request.getScheduledStartAt())
                .scheduledEndAt(request.getScheduledEndAt())
                .status(initialStatus(request.getMeetingType()))
                .build();

        meeting = meetingRepository.save(meeting);
        participantRepository.save(MeetingParticipant.builder()
                .meeting(meeting)
                .user(host)
                .role(ParticipantRole.HOST)
                .joinSource(JoinSource.HOST)
                .participationStatus(ParticipationStatus.APPROVED)
                .approvedAt(Instant.now())
                .approvedBy(host)
                .build());

        return meetingMapper.toMeetingResponse(meeting);
    }

    public MeetingResponse getMeeting(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireCanView(meeting, userId, admin);
        return meetingMapper.toMeetingResponse(meeting);
    }

    public PageResponse<MeetingResponse> getMyMeetings(String userId, MeetingStatus status, MeetingType type) {
        findUser(userId);
        List<MeetingResponse> meetings = participantRepository.findMyMeetings(
                        userId,
                        status,
                        type,
                        ParticipationStatus.INVITED,
                        InvitationStatus.ACCEPTED)
                .stream()
                .map(MeetingParticipant::getMeeting)
                .map(meetingMapper::toMeetingResponse)
                .toList();

        return PageResponse.<MeetingResponse>builder()
                .items(meetings)
                .total((long) meetings.size())
                .build();
    }

    @Transactional
    public MeetingResponse updateMeeting(String meetingId, MeetingUpdateRequest request, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);
        requireMutable(meeting);
        validateSchedule(meeting.getMeetingType(), request.getScheduledStartAt(), request.getScheduledEndAt());

        meeting.setTitle(request.getTitle().trim());
        meeting.setDescription(request.getDescription());
        meeting.setScheduledStartAt(request.getScheduledStartAt());
        meeting.setScheduledEndAt(request.getScheduledEndAt());
        return meetingMapper.toMeetingResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public MeetingResponse cancelMeeting(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);
        requireCanCancel(meeting);
        meeting.setStatus(MeetingStatus.CANCELLED);
        return meetingMapper.toMeetingResponse(meetingRepository.save(meeting));
    }

    @Transactional
    public MeetingResponse endMeeting(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);
        requireMutable(meeting);
        meeting.setStatus(MeetingStatus.ENDED);
        return meetingMapper.toMeetingResponse(meetingRepository.save(meeting));
    }

    public JoinMeetingResponse findByJoinCode(String displayJoinCode) {
        String joinCode = normalizeJoinCode(displayJoinCode);
        Meeting meeting = meetingRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
        return meetingMapper.toJoinMeetingResponse(meeting);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private Meeting findMeeting(String meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
    }

    private MeetingStatus initialStatus(MeetingType meetingType) {
        return meetingType == MeetingType.INSTANT ? MeetingStatus.ACTIVE : MeetingStatus.SCHEDULED;
    }

    private void validateSchedule(MeetingType type, Instant startAt, Instant endAt) {
        if (type == MeetingType.SCHEDULED
                && (startAt == null || endAt == null || !endAt.isAfter(startAt))) {
            throw new AppException(ErrorCode.MEETING_TIME_INVALID);
        }
        if (type == MeetingType.INSTANT && (startAt != null || endAt != null)) {
            throw new AppException(ErrorCode.MEETING_TIME_INVALID);
        }
    }

    private void requireCanView(Meeting meeting, String userId, boolean admin) {
        if (admin) {
            return;
        }

        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ACCESS_DENIED));
        // những người được mời nhưng chưa chấp nhận sẽ không thể xem thông tin cuộc họp
        if (participant.getParticipationStatus() == ParticipationStatus.INVITED
                && !hasAcceptedInvitation(meeting.getId(), userId)) {
            throw new AppException(ErrorCode.MEETING_ACCESS_DENIED);
        }
    }

    private boolean hasAcceptedInvitation(String meetingId, String userId) {
        return invitationRepository.existsByMeeting_IdAndInvitee_IdAndStatus(
                meetingId,
                userId,
                InvitationStatus.ACCEPTED);
    }

    private void requireHostOrAdmin(Meeting meeting, String userId, boolean admin) {
        if (!admin && !meeting.getHost().getId().equals(userId)) {
            throw new AppException(ErrorCode.MEETING_ACCESS_DENIED);
        }
    }

    private void requireMutable(Meeting meeting) {
        if (meeting.getStatus() == MeetingStatus.CANCELLED || meeting.getStatus() == MeetingStatus.ENDED) {
            throw new AppException(ErrorCode.MEETING_STATE_INVALID);
        }
    }

    private void requireCanCancel(Meeting meeting) {
        if (meeting.getMeetingType() != MeetingType.SCHEDULED || meeting.getStatus() != MeetingStatus.SCHEDULED) {
            throw new AppException(ErrorCode.MEETING_STATE_INVALID);
        }
    }

    private String generateJoinCode() {
        for (int attempt = 0; attempt < JOIN_CODE_GENERATION_ATTEMPTS; attempt++) {
            StringBuilder result = new StringBuilder(JOIN_CODE_LENGTH);
            for (int index = 0; index < JOIN_CODE_LENGTH; index++) {
                result.append(JOIN_CODE_CHARSET.charAt(secureRandom.nextInt(JOIN_CODE_CHARSET.length())));
            }
            String candidate = result.toString();
            if (!meetingRepository.existsByJoinCode(candidate)) {
                return candidate;
            }
        }
        throw new AppException(ErrorCode.JOIN_CODE_GENERATION_FAILED);
    }

    private String normalizeJoinCode(String joinCode) {
        if (joinCode == null) {
            throw new AppException(ErrorCode.JOIN_CODE_INVALID);
        }
        String normalized = joinCode.replace("-", "").toUpperCase(Locale.ROOT);
        if (normalized.length() != JOIN_CODE_LENGTH
                || normalized.chars().anyMatch(character -> JOIN_CODE_CHARSET.indexOf(character) < 0)) {
            throw new AppException(ErrorCode.JOIN_CODE_INVALID);
        }
        return normalized;
    }
}
