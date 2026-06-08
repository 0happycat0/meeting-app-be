package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.MeetingInvitationCreationRequest;
import com.happycat.meetingappbe.dto.response.MeetingInvitationResponse;
import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.entity.MeetingInvitation;
import com.happycat.meetingappbe.entity.MeetingParticipant;
import com.happycat.meetingappbe.entity.User;
import com.happycat.meetingappbe.enums.*;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.mapper.MeetingInvitationMapper;
import com.happycat.meetingappbe.repository.MeetingInvitationRepository;
import com.happycat.meetingappbe.repository.MeetingParticipantRepository;
import com.happycat.meetingappbe.repository.MeetingRepository;
import com.happycat.meetingappbe.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingInvitationService {
    private static final List<InvitationStatus> ACTIVE_INVITATION_STATUSES =
            List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED);
    private static final Logger log = LoggerFactory.getLogger(MeetingInvitationService.class);

    MeetingInvitationRepository invitationRepository;
    MeetingRepository meetingRepository;
    MeetingParticipantRepository participantRepository;
    UserRepository userRepository;
    MeetingInvitationMapper invitationMapper;

    @Transactional
    public MeetingInvitationResponse createInvitation(
            String meetingId,
            MeetingInvitationCreationRequest request,
            String inviterId,
            boolean admin
    ) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, inviterId, admin);
        requireMeetingOpen(meeting);

        User inviter = findUser(inviterId);
        User invitee = findUser(request.getInviteeId());
        // không được tự mời chính mình
        if (meeting.getHost().getId().equals(invitee.getId())) {
            throw new AppException(ErrorCode.INVITATION_INVITEE_INVALID);
        }
        // không được mời người đã được mời hoặc đã chấp nhận
        if (invitationRepository.existsByMeeting_IdAndInvitee_IdAndStatusIn(
                meeting.getId(), invitee.getId(), ACTIVE_INVITATION_STATUSES)) {
            throw new AppException(ErrorCode.INVITATION_DUPLICATED);
        }
        // thêm người được mời vào danh sách tham gia với trạng thái INVITED
        ensureInvitedParticipant(meeting, invitee);

        MeetingInvitation invitation = MeetingInvitation.builder()
                .meeting(meeting)
                .inviter(inviter)
                .invitee(invitee)
                .status(InvitationStatus.PENDING)
                .build();

        return invitationMapper.toResponse(invitationRepository.save(invitation));
    }

    public PageResponse<MeetingInvitationResponse> getMeetingInvitations(
            String meetingId,
            String userId,
            boolean admin
    ) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);

        List<MeetingInvitationResponse> invitations = invitationRepository.findByMeeting_IdOrderBySentAtDesc(meetingId)
                .stream()
                .map(invitationMapper::toResponse)
                .toList();

        return PageResponse.<MeetingInvitationResponse>builder()
                .items(invitations)
                .total((long) invitations.size())
                .build();
    }

    public PageResponse<MeetingInvitationResponse> getMyInvitations(String userId) {
        findUser(userId);

        List<MeetingInvitationResponse> invitations = invitationRepository.findByInvitee_IdOrderBySentAtDesc(userId)
                .stream()
                .map(invitationMapper::toResponse)
                .toList();

        return PageResponse.<MeetingInvitationResponse>builder()
                .items(invitations)
                .total((long) invitations.size())
                .build();
    }

    @Transactional
    public MeetingInvitationResponse acceptInvitation(String invitationId, String userId) {
        MeetingInvitation invitation = findInvitation(invitationId);
        requireInvitee(invitation, userId);
        requirePending(invitation);
        requireMeetingOpen(invitation.getMeeting());

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(Instant.now());
        ensureParticipantVisible(invitation.getMeeting(), invitation.getInvitee());

        return invitationMapper.toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public MeetingInvitationResponse declineInvitation(String invitationId, String userId) {
        MeetingInvitation invitation = findInvitation(invitationId);
        requireInvitee(invitation, userId);
        requirePending(invitation);

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setRespondedAt(Instant.now());
        removeInvitedParticipant(invitation.getMeeting(), invitation.getInvitee());

        return invitationMapper.toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public MeetingInvitationResponse cancelInvitation(String invitationId, String userId, boolean admin) {
        MeetingInvitation invitation = findInvitation(invitationId);
        requireHostOrAdmin(invitation.getMeeting(), userId, admin);
        requirePending(invitation);

        invitation.setStatus(InvitationStatus.CANCELLED);
        invitation.setRespondedAt(Instant.now());
        removeInvitedParticipant(invitation.getMeeting(), invitation.getInvitee());

        return invitationMapper.toResponse(invitationRepository.save(invitation));
    }

    private Meeting findMeeting(String meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
    }

    private MeetingInvitation findInvitation(String invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_NOT_FOUND));
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

    private void requireInvitee(MeetingInvitation invitation, String userId) {
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new AppException(ErrorCode.INVITATION_ACCESS_DENIED);
        }
    }

    private void requirePending(MeetingInvitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AppException(ErrorCode.INVITATION_STATE_INVALID);
        }
    }

    private void requireMeetingOpen(Meeting meeting) {
        if (meeting.getStatus() == MeetingStatus.CANCELLED || meeting.getStatus() == MeetingStatus.ENDED) {
            log.info("Meeting status: {}", meeting.getStatus());
            throw new AppException(ErrorCode.MEETING_STATE_INVALID);
        }
    }

    private void ensureInvitedParticipant(Meeting meeting, User invitee) {
        participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), invitee.getId())
                .ifPresentOrElse(participant -> {
                    if (participant.getParticipationStatus() != ParticipationStatus.INVITED) {
                        throw new AppException(ErrorCode.INVITATION_DUPLICATED);
                    }
                    participant.setJoinSource(JoinSource.INVITATION);
                    participantRepository.save(participant);
                }, () -> participantRepository.save(MeetingParticipant.builder()
                        .meeting(meeting)
                        .user(invitee)
                        .role(ParticipantRole.PARTICIPANT)
                        .joinSource(JoinSource.INVITATION)
                        .participationStatus(ParticipationStatus.INVITED)
                        .build()));
    }

    private void ensureParticipantVisible(Meeting meeting, User invitee) {
        if (participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), invitee.getId()).isEmpty()) {
            participantRepository.save(MeetingParticipant.builder()
                    .meeting(meeting)
                    .user(invitee)
                    .role(ParticipantRole.PARTICIPANT)
                    .joinSource(JoinSource.INVITATION)
                    .participationStatus(ParticipationStatus.INVITED)
                    .build());
        }
    }

    private void removeInvitedParticipant(Meeting meeting, User invitee) {
        participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), invitee.getId())
                .filter(participant -> participant.getParticipationStatus() == ParticipationStatus.INVITED)
                .ifPresent(participantRepository::delete);
    }
}
