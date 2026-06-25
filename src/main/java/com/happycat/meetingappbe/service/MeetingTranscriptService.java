package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.TranscriptSegmentBatchRequest;
import com.happycat.meetingappbe.dto.request.TranscriptSegmentRequest;
import com.happycat.meetingappbe.dto.response.MeetingTranscriptSegmentResponse;
import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.entity.MeetingParticipant;
import com.happycat.meetingappbe.entity.MeetingTranscriptSegment;
import com.happycat.meetingappbe.enums.MeetingStatus;
import com.happycat.meetingappbe.enums.ParticipationStatus;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.mapper.MeetingTranscriptSegmentMapper;
import com.happycat.meetingappbe.repository.MeetingParticipantRepository;
import com.happycat.meetingappbe.repository.MeetingRepository;
import com.happycat.meetingappbe.repository.MeetingTranscriptSegmentRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingTranscriptService {
    MeetingRepository meetingRepository;
    MeetingParticipantRepository participantRepository;
    MeetingTranscriptSegmentRepository transcriptSegmentRepository;
    MeetingTranscriptSegmentMapper transcriptSegmentMapper;
    MeetingTranscriptEventService transcriptEventService;

    @Transactional
    public PageResponse<MeetingTranscriptSegmentResponse> saveTranscriptSegments(
            String meetingId,
            String userId,
            TranscriptSegmentBatchRequest request
    ) {
        if (request.getSegments() == null || request.getSegments().isEmpty()) {
            throw new AppException(ErrorCode.TRANSCRIPT_SEGMENT_INVALID);
        }

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new AppException(ErrorCode.MEETING_NOT_FOUND));
        requireMeetingCanAcceptTranscript(meeting);
        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meetingId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTICIPANT_NOT_FOUND));
        requireCanUploadTranscript(participant);

        List<TranscriptSegmentSaveResult> saveResults = request.getSegments().stream()
                .map(segmentRequest -> saveIfNeeded(meeting, participant, segmentRequest))
                .toList();
        List<MeetingTranscriptSegmentResponse> responses = saveResults.stream()
                .map(TranscriptSegmentSaveResult::segment)
                .map(transcriptSegmentMapper::toResponse)
                .toList();

        for (int index = 0; index < saveResults.size(); index++) {
            if (saveResults.get(index).created()) {
                transcriptEventService.broadcastSegment(meetingId, userId, responses.get(index));
            }
        }

        return PageResponse.<MeetingTranscriptSegmentResponse>builder()
                .items(responses)
                .total((long) responses.size())
                .build();
    }

    private TranscriptSegmentSaveResult saveIfNeeded(
            Meeting meeting,
            MeetingParticipant participant,
            TranscriptSegmentRequest request
    ) {
        return transcriptSegmentRepository.findByMeeting_IdAndParticipant_IdAndSegmentId(
                        meeting.getId(),
                        participant.getId(),
                        request.getSegmentId())
                .map(existing -> new TranscriptSegmentSaveResult(existing, false))
                .orElseGet(() -> new TranscriptSegmentSaveResult(transcriptSegmentRepository.save(
                        MeetingTranscriptSegment.builder()
                        .meeting(meeting)
                        .participant(participant)
                        .user(participant.getUser())
                        .segmentId(request.getSegmentId())
                        .text(request.getText().trim())
                        .latencyMsFromFirstAudio(request.getLatencyMsFromFirstAudio())
                        .tokenCount(request.getTokenCount())
                        .totalTokensEmitted(request.getTotalTokensEmitted())
                        .clientCreatedAt(request.getClientCreatedAt())
                        .build()), true));
    }

    private void requireCanUploadTranscript(MeetingParticipant participant) {
        ParticipationStatus status = participant.getParticipationStatus();
        if (status != ParticipationStatus.JOINED) {
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }
    }

    private void requireMeetingCanAcceptTranscript(Meeting meeting) {
        if (meeting.getStatus() == MeetingStatus.ENDED || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new AppException(ErrorCode.MEETING_STATE_INVALID);
        }
    }

    private record TranscriptSegmentSaveResult(MeetingTranscriptSegment segment, boolean created) {
    }
}
