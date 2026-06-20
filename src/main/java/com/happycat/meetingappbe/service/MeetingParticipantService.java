package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.LiveKitJoinTokenRequest;
import com.happycat.meetingappbe.dto.response.LiveKitJoinTokenResponse;
import com.happycat.meetingappbe.dto.response.MeetingParticipantResponse;
import com.happycat.meetingappbe.entity.*;
import com.happycat.meetingappbe.enums.*;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.mapper.MeetingParticipantMapper;
import com.happycat.meetingappbe.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingParticipantService {
    private static final List<InvitationStatus> REQUESTABLE_INVITATION_STATUSES =
            List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED);

    MeetingRepository meetingRepository;
    MeetingParticipantRepository participantRepository;
    MeetingInvitationRepository invitationRepository;
    MeetingTokenLogRepository tokenLogRepository;
    UserRepository userRepository;
    MeetingParticipantMapper participantMapper;
    LiveKitAccessService liveKitAccessService;
    MeetingTranscriptEventService transcriptEventService;

    @Transactional
    public MeetingParticipantResponse requestWaitingRoomByJoinCode(String displayJoinCode, String userId) {
        Meeting meeting = findMeetingByJoinCode(displayJoinCode);
        requireMeetingOpen(meeting);
        User user = findUser(userId);
        acceptPendingInvitationIfExists(meeting.getId(), userId);

        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), userId)
                .map(existing -> moveExistingToWaiting(existing, JoinSource.JOIN_CODE))
                .orElseGet(() -> createWaitingParticipant(meeting, user, JoinSource.JOIN_CODE));

        return participantMapper.toResponse(participantRepository.save(participant));
    }

    @Transactional
    public MeetingParticipantResponse requestWaitingRoomByInvitation(String invitationId, String userId) {
        MeetingInvitation invitation = findInvitation(invitationId);
        requireInvitee(invitation, userId);
        requireRequestableInvitation(invitation);
        Meeting meeting = invitation.getMeeting();
        requireMeetingOpen(meeting);
        User user = findUser(userId);
        acceptInvitationIfPending(invitation);

        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), userId)
                .map(existing -> moveExistingToWaiting(existing, JoinSource.INVITATION))
                .orElseGet(() -> createWaitingParticipant(meeting, user, JoinSource.INVITATION));

        return participantMapper.toResponse(participantRepository.save(participant));
    }

    @Transactional
    public PageResponse<MeetingParticipantResponse> getWaitingRoom(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);

        List<MeetingParticipantResponse> participants = participantRepository
                .findByMeeting_IdAndParticipationStatusOrderByRequestedAtAsc(
                        meetingId,
                        ParticipationStatus.WAITING_APPROVAL)
                .stream()
                .map(participantMapper::toResponse)
                .toList();

        return toPage(participants);
    }

    @Transactional
    public MeetingParticipantResponse approveParticipant(
            String meetingId,
            String participantId,
            String approverId,
            boolean admin
    ) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, approverId, admin);
        requireMeetingOpen(meeting);

        MeetingParticipant participant = findParticipant(participantId, meetingId);
        if (participant.getRole() == ParticipantRole.HOST
                || participant.getParticipationStatus() != ParticipationStatus.WAITING_APPROVAL) {
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }

        User approver = findUser(approverId);
        participant.setParticipationStatus(ParticipationStatus.APPROVED);
        participant.setApprovedAt(Instant.now());
        participant.setApprovedBy(approver);

        return participantMapper.toResponse(participantRepository.save(participant));
    }

    @Transactional
    public MeetingParticipantResponse rejectParticipant(
            String meetingId,
            String participantId,
            String rejecterId,
            boolean admin
    ) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, rejecterId, admin);
        requireMeetingOpen(meeting);

        MeetingParticipant participant = findParticipant(participantId, meetingId);
        if (participant.getRole() == ParticipantRole.HOST
                || participant.getParticipationStatus() != ParticipationStatus.WAITING_APPROVAL) {
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }

        participant.setParticipationStatus(ParticipationStatus.REJECTED);

        return participantMapper.toResponse(participantRepository.save(participant));
    }

    @Transactional
    public PageResponse<MeetingParticipantResponse> getParticipants(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireCanListParticipants(meeting, userId, admin);

        List<MeetingParticipantResponse> participants = participantRepository
                .findByMeeting_IdOrderByCreatedAtAsc(meetingId)
                .stream()
                .map(participantMapper::toResponse)
                .toList();

        return toPage(participants);
    }

    @Transactional
    public MeetingParticipantResponse getMyStatus(String meetingId, String userId) {
        findMeeting(meetingId);
        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meetingId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_FOUND));

        return participantMapper.toResponse(participant);
    }

    @Transactional
    public LiveKitJoinTokenResponse issueJoinToken(
            String meetingId,
            String userId,
            LiveKitJoinTokenRequest request
    ) {
        Meeting meeting = findMeeting(meetingId);
        requireMeetingOpen(meeting);

        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meetingId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_FOUND));
        requireCanIssueToken(participant);

        String tokenIdentifier = UUID.randomUUID().toString();
        try {
            LiveKitTokenIssue tokenIssue = liveKitAccessService.createJoinToken(
                    meeting,
                    participant.getUser(),
                    participant.getRole(),
                    tokenIdentifier,
                    requestedDisplayName(request));
            participant.setParticipationStatus(ParticipationStatus.JOINED);
            if (participant.getJoinedAt() == null) {
                participant.setJoinedAt(Instant.now());
            }
            participant.setLeftAt(null);
            participantRepository.save(participant);
            tokenLogRepository.save(successTokenLog(meeting, participant.getUser(), tokenIdentifier, tokenIssue));

            return LiveKitJoinTokenResponse.builder()
                    .liveKitUrl(tokenIssue.liveKitUrl())
                    .token(tokenIssue.token())
                    .roomName(tokenIssue.roomName())
                    .identity(tokenIssue.identity())
                    .expiresAt(tokenIssue.expiresAt())
                    .build();
        } catch (RuntimeException exception) {
            tokenLogRepository.save(failedTokenLog(meeting, participant.getUser(), tokenIdentifier, exception));
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.LIVEKIT_SERVICE_UNAVAILABLE);
        }
    }

    @Transactional
    public MeetingParticipantResponse leaveMeeting(String meetingId, String userId) {
        findMeeting(meetingId);
        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meetingId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_FOUND));
        if (participant.getParticipationStatus() != ParticipationStatus.JOINED) {
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }

        participant.setParticipationStatus(ParticipationStatus.LEFT);
        participant.setLeftAt(Instant.now());

        MeetingParticipant savedParticipant = participantRepository.save(participant);
        transcriptEventService.disconnectUser(meetingId, userId);

        return participantMapper.toResponse(savedParticipant);
    }

    @Transactional
    public MeetingParticipantResponse removeParticipant(
            String meetingId,
            String participantId,
            String removerId,
            boolean admin
    ) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, removerId, admin);
        requireMeetingOpen(meeting);

        MeetingParticipant participant = findParticipant(participantId, meetingId);
        if (participant.getRole() == ParticipantRole.HOST
                || participant.getParticipationStatus() != ParticipationStatus.JOINED) {
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }

        User remover = findUser(removerId);
        liveKitAccessService.removeParticipant(meeting.getRoomId(), participant.getUser().getId());
        participant.setParticipationStatus(ParticipationStatus.REMOVED);
        participant.setRemovedAt(Instant.now());
        participant.setRemovedBy(remover);

        MeetingParticipant savedParticipant = participantRepository.save(participant);
        transcriptEventService.disconnectUser(meetingId, participant.getUser().getId());

        return participantMapper.toResponse(savedParticipant);
    }

    private MeetingParticipant moveExistingToWaiting(MeetingParticipant participant, JoinSource joinSource) {
        return switch (participant.getParticipationStatus()) {
            case INVITED, LEFT -> markWaiting(participant, joinSource);
            case WAITING_APPROVAL, APPROVED, JOINED -> participant;
            case REJECTED, REMOVED -> throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        };
    }

    private MeetingParticipant createWaitingParticipant(Meeting meeting, User user, JoinSource joinSource) {
        if (meeting.getHost().getId().equals(user.getId())) {
            return MeetingParticipant.builder()
                    .meeting(meeting)
                    .user(user)
                    .role(ParticipantRole.HOST)
                    .joinSource(JoinSource.HOST)
                    .participationStatus(ParticipationStatus.APPROVED)
                    .approvedAt(Instant.now())
                    .approvedBy(user)
                    .build();
        }

        return MeetingParticipant.builder()
                .meeting(meeting)
                .user(user)
                .role(ParticipantRole.PARTICIPANT)
                .joinSource(joinSource)
                .participationStatus(ParticipationStatus.WAITING_APPROVAL)
                .requestedAt(Instant.now())
                .build();
    }

    private MeetingParticipant markWaiting(MeetingParticipant participant, JoinSource joinSource) {
        participant.setJoinSource(joinSource);
        participant.setParticipationStatus(ParticipationStatus.WAITING_APPROVAL);
        participant.setRequestedAt(Instant.now());
        participant.setApprovedAt(null);
        participant.setApprovedBy(null);
        participant.setJoinedAt(null);
        participant.setLeftAt(null);
        return participant;
    }

    private void requireCanIssueToken(MeetingParticipant participant) {
        ParticipationStatus participationStatus = participant.getParticipationStatus();
        if (participant.getRole() == ParticipantRole.HOST
                && participationStatus != ParticipationStatus.REMOVED
                && participationStatus != ParticipationStatus.REJECTED) {
            return;
        }
        if (participationStatus != ParticipationStatus.APPROVED
                && participationStatus != ParticipationStatus.JOINED) {
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }
    }

    private String requestedDisplayName(LiveKitJoinTokenRequest request) {
        if (request == null) {
            return null;
        }
        return request.getName();
    }

    private MeetingTokenLog successTokenLog(
            Meeting meeting,
            User user,
            String tokenIdentifier,
            LiveKitTokenIssue tokenIssue
    ) {
        return MeetingTokenLog.builder()
                .meeting(meeting)
                .user(user)
                .liveKitRoomId(meeting.getRoomId())
                .tokenIdentifier(tokenIdentifier)
                .issuedAt(Instant.now())
                .expiresAt(tokenIssue.expiresAt())
                .issueStatus(TokenIssueStatus.SUCCESS)
                .build();
    }

    private MeetingTokenLog failedTokenLog(
            Meeting meeting,
            User user,
            String tokenIdentifier,
            RuntimeException exception
    ) {
        return MeetingTokenLog.builder()
                .meeting(meeting)
                .user(user)
                .liveKitRoomId(meeting.getRoomId())
                .tokenIdentifier(tokenIdentifier)
                .issuedAt(Instant.now())
                .issueStatus(TokenIssueStatus.FAILED)
                .failureReason(exception.getClass().getSimpleName())
                .build();
    }

    private void acceptInvitationIfPending(MeetingInvitation invitation) {
        if (invitation.getStatus() == InvitationStatus.PENDING) {
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setRespondedAt(Instant.now());
            invitationRepository.save(invitation);
        }
    }

    private void acceptPendingInvitationIfExists(String meetingId, String userId) {
        invitationRepository.findFirstByMeeting_IdAndInvitee_IdAndStatusInOrderBySentAtDesc(
                        meetingId,
                        userId,
                        List.of(InvitationStatus.PENDING))
                .ifPresent(this::acceptInvitationIfPending);
    }

    private void requireInvitee(MeetingInvitation invitation, String userId) {
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new AppException(ErrorCode.INVITATION_ACCESS_DENIED);
        }
    }

    private void requireRequestableInvitation(MeetingInvitation invitation) {
        if (!REQUESTABLE_INVITATION_STATUSES.contains(invitation.getStatus())) {
            throw new AppException(ErrorCode.INVITATION_STATE_INVALID);
        }
    }

    private void requireCanListParticipants(Meeting meeting, String userId, boolean admin) {
        if (admin || meeting.getHost().getId().equals(userId)) {
            return;
        }

        MeetingParticipant caller = participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_ACCESS_DENIED));
        if (caller.getParticipationStatus() != ParticipationStatus.APPROVED
                && caller.getParticipationStatus() != ParticipationStatus.JOINED) {
            throw new AppException(ErrorCode.MEETING_ACCESS_DENIED);
        }
    }

    private Meeting findMeeting(String meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
    }

    private Meeting findMeetingByJoinCode(String displayJoinCode) {
        return meetingRepository.findByJoinCode(normalizeJoinCode(displayJoinCode))
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
    }

    private MeetingInvitation findInvitation(String invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));
    }

    private MeetingParticipant findParticipant(String participantId, String meetingId) {
        return participantRepository.findByIdAndMeeting_Id(participantId, meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void requireHostOrAdmin(Meeting meeting, String userId, boolean admin) {
        if (!admin && !meeting.getHost().getId().equals(userId)) {
            throw new AppException(ErrorCode.MEETING_ACCESS_DENIED);
        }
    }

    private void requireMeetingOpen(Meeting meeting) {
        if (meeting.getStatus() == MeetingStatus.CANCELLED || meeting.getStatus() == MeetingStatus.ENDED) {
            throw new AppException(ErrorCode.MEETING_STATE_INVALID);
        }
    }

    private String normalizeJoinCode(String joinCode) {
        if (joinCode == null) {
            throw new AppException(ErrorCode.JOIN_CODE_INVALID);
        }
        String normalized = joinCode.replace("-", "").toUpperCase(Locale.ROOT);
        if (normalized.length() != 8
                || normalized.chars().anyMatch(character -> MeetingService.JOIN_CODE_CHARSET.indexOf(character) < 0)) {
            throw new AppException(ErrorCode.JOIN_CODE_INVALID);
        }
        return normalized;
    }

    private PageResponse<MeetingParticipantResponse> toPage(List<MeetingParticipantResponse> participants) {
        return PageResponse.<MeetingParticipantResponse>builder()
                .items(participants)
                .total((long) participants.size())
                .build();
    }
}
