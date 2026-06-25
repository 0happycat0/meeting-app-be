package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.configuration.LlmConfig;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.response.MeetingMinutesListItemResponse;
import com.happycat.meetingappbe.dto.response.MeetingMinutesResponse;
import com.happycat.meetingappbe.entity.*;
import com.happycat.meetingappbe.enums.*;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.mapper.MeetingMinutesMapper;
import com.happycat.meetingappbe.repository.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingMinutesService {
    MeetingRepository meetingRepository;
    MeetingParticipantRepository participantRepository;
    MeetingTranscriptSegmentRepository transcriptSegmentRepository;
    MeetingMinutesRepository minutesRepository;
    UserRepository userRepository;
    MeetingMinutesMapper minutesMapper;
    MeetingMinutesWorker minutesWorker;
    LlmConfig llmConfig;

    @Transactional
    public PageResponse<MeetingMinutesListItemResponse> getMyMinutes(String userId, boolean admin) {
        findUser(userId);
        List<Meeting> meetings = findVisibleEndedMeetings(userId, admin);
        Map<String, MeetingMinutes> minutesByMeetingId = findMinutesByMeetingId(meetings);

        List<MeetingMinutesListItemResponse> items = meetings.stream()
                .map(meeting -> toListItem(meeting, minutesByMeetingId.get(meeting.getId()), userId, admin))
                .toList();

        return PageResponse.<MeetingMinutesListItemResponse>builder()
                .items(items)
                .total((long) items.size())
                .build();
    }

    @Transactional
    public MeetingMinutesResponse generateMinutes(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);
        requireEnded(meeting);

        List<MeetingTranscriptSegment> transcriptSegments = transcriptSegmentRepository.findForMinutesByMeetingId(meetingId);
        if (transcriptSegments.isEmpty()) {
            throw new AppException(ErrorCode.MEETING_MINUTES_NO_TRANSCRIPT);
        }

        User generator = findUser(userId);
        MeetingMinutes minutes = minutesRepository.findByMeeting_Id(meetingId)
                .orElseGet(() -> MeetingMinutes.builder()
                        .meeting(meeting)
                        .build());
        minutes.setStatus(MeetingMinutesStatus.GENERATING);
        minutes.setContentMarkdown(null);
        minutes.setPublished(false);
        minutes.setModel(llmConfig.getModel());
        minutes.setGeneratedBy(generator);
        minutes.setGeneratedAt(Instant.now());
        minutes.setSourceSegmentCount(transcriptSegments.size());
        minutes.setChunkCount(0);
        minutes.setFailureReason(null);

        MeetingMinutes savedMinutes = minutesRepository.save(minutes);
        scheduleGenerationAfterCommit(savedMinutes.getId());
        return minutesMapper.toResponse(savedMinutes);
    }

    @Transactional
    public MeetingMinutesResponse getMinutes(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        MeetingMinutes minutes = minutesRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND));
        requireCanViewMinutes(meeting, minutes, userId, admin);
        return minutesMapper.toResponse(minutes);
    }

    @Transactional
    public MeetingMinutesResponse publishMinutes(String meetingId, String userId, boolean admin) {
        Meeting meeting = findMeeting(meetingId);
        requireHostOrAdmin(meeting, userId, admin);
        MeetingMinutes minutes = minutesRepository.findByMeeting_Id(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND));
        if (minutes.getStatus() != MeetingMinutesStatus.COMPLETED) {
            throw new AppException(ErrorCode.MEETING_MINUTES_STATE_INVALID);
        }
        minutes.setPublished(true);
        return minutesMapper.toResponse(minutesRepository.save(minutes));
    }

    private Meeting findMeeting(String meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void requireEnded(Meeting meeting) {
        if (meeting.getStatus() != MeetingStatus.ENDED) {
            throw new AppException(ErrorCode.MEETING_STATE_INVALID);
        }
    }

    private void requireHostOrAdmin(Meeting meeting, String userId, boolean admin) {
        if (!admin && !meeting.getHost().getId().equals(userId)) {
            throw new AppException(ErrorCode.MEETING_ACCESS_DENIED);
        }
    }

    private void requireCanViewMinutes(Meeting meeting, MeetingMinutes minutes, String userId, boolean admin) {
        if (admin || meeting.getHost().getId().equals(userId)) {
            return;
        }
        if (!minutes.isPublished()) {
            throw new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND);
        }
        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND));
        if (participant.getParticipationStatus() == ParticipationStatus.REMOVED) {
            throw new AppException(ErrorCode.MEETING_MINUTES_NOT_FOUND);
        }
    }

    private List<Meeting> findVisibleEndedMeetings(String userId, boolean admin) {
        if (admin) {
            return meetingRepository.findByStatusWithHost(MeetingStatus.ENDED);
        }
        return participantRepository.findMyMeetings(
                        userId,
                        MeetingStatus.ENDED,
                        null,
                        ParticipationStatus.INVITED,
                        ParticipationStatus.REMOVED,
                        InvitationStatus.ACCEPTED)
                .stream()
                .map(MeetingParticipant::getMeeting)
                .toList();
    }

    private Map<String, MeetingMinutes> findMinutesByMeetingId(List<Meeting> meetings) {
        if (meetings.isEmpty()) {
            return Map.of();
        }
        List<String> meetingIds = meetings.stream()
                .map(Meeting::getId)
                .toList();
        return minutesRepository.findByMeeting_IdIn(meetingIds).stream()
                .collect(Collectors.toMap(minutes -> minutes.getMeeting().getId(), Function.identity()));
    }

    private MeetingMinutesListItemResponse toListItem(
            Meeting meeting,
            MeetingMinutes minutes,
            String userId,
            boolean admin
    ) {
        boolean visibleMinutes = canExposeMinutesInList(meeting, minutes, userId, admin);
        MeetingMinutesListStatus minutesStatus = visibleMinutes
                ? MeetingMinutesListStatus.valueOf(minutes.getStatus().name())
                : MeetingMinutesListStatus.NONE;

        return MeetingMinutesListItemResponse.builder()
                .meetingId(meeting.getId())
                .meetingTitle(meeting.getTitle())
                .meetingDescription(meeting.getDescription())
                .meetingType(meeting.getMeetingType())
                .hostId(meeting.getHost().getId())
                .hostFirstName(meeting.getHost().getFirstName())
                .hostLastName(meeting.getHost().getLastName())
                .meetingStatus(meeting.getStatus())
                .scheduledStartAt(meeting.getScheduledStartAt())
                .scheduledEndAt(meeting.getScheduledEndAt())
                .minutesId(visibleMinutes ? minutes.getId() : null)
                .minutesStatus(minutesStatus)
                .published(visibleMinutes && minutes.isPublished())
                .generatedAt(visibleMinutes ? minutes.getGeneratedAt() : null)
                .updatedAt(visibleMinutes ? minutes.getUpdatedAt() : null)
                .build();
    }

    private boolean canExposeMinutesInList(Meeting meeting, MeetingMinutes minutes, String userId, boolean admin) {
        if (minutes == null) {
            return false;
        }
        if (admin || meeting.getHost().getId().equals(userId)) {
            return true;
        }
        return minutes.isPublished();
    }

    private void scheduleGenerationAfterCommit(String minutesId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            minutesWorker.generateAsync(minutesId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                minutesWorker.generateAsync(minutesId);
            }
        });
    }
}
