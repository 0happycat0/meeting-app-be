package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.dto.response.MeetingTranscriptSegmentResponse;
import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.entity.MeetingParticipant;
import com.happycat.meetingappbe.enums.ParticipationStatus;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.repository.MeetingParticipantRepository;
import com.happycat.meetingappbe.repository.MeetingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MeetingTranscriptEventService {
    static final String TRANSCRIPT_SEGMENT_CREATED_EVENT = "transcript.segment.created";

    MeetingRepository meetingRepository;
    MeetingParticipantRepository participantRepository;
    ConcurrentMap<String, ConcurrentMap<String, SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String meetingId, String userId) {
        log.debug("Transcript SSE subscribe requested: meetingId={}, userId={}", meetingId, userId);
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> {
                    log.debug("Transcript SSE subscribe denied, meeting not found: meetingId={}, userId={}",
                            meetingId, userId);
                    return new AppException(ErrorCode.MEETING_NOT_FOUND);
                });
        MeetingParticipant participant = participantRepository.findByMeeting_IdAndUser_Id(meeting.getId(), userId)
                .orElseThrow(() -> {
                    log.debug("Transcript SSE subscribe denied, participant not found: meetingId={}, userId={}",
                            meetingId, userId);
                    return new AppException(ErrorCode.PARTICIPANT_NOT_FOUND);
                });
        if (participant.getParticipationStatus() != ParticipationStatus.JOINED) {
            log.debug(
                    "Transcript SSE subscribe denied, participant is not joined: meetingId={}, userId={}, status={}",
                    meetingId,
                    userId,
                    participant.getParticipationStatus()
            );
            throw new AppException(ErrorCode.PARTICIPANT_STATE_INVALID);
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ConcurrentMap<String, SseEmitter> meetingEmitters = emitters
                .computeIfAbsent(meetingId, key -> new ConcurrentHashMap<>());
        SseEmitter previousEmitter = meetingEmitters.put(userId, emitter);
        log.debug(
                "Transcript SSE emitter registered: meetingId={}, userId={}, replaced={}, subscriberCount={}",
                meetingId,
                userId,
                previousEmitter != null,
                meetingEmitters.size()
        );
        if (previousEmitter != null) {
            previousEmitter.complete();
            log.debug("Transcript SSE previous emitter completed: meetingId={}, userId={}", meetingId, userId);
        }

        emitter.onCompletion(() -> {
            log.debug("Transcript SSE emitter completion callback: meetingId={}, userId={}", meetingId, userId);
            removeEmitter(meetingId, userId, emitter);
        });
        emitter.onTimeout(() -> {
            log.debug("Transcript SSE emitter timeout callback: meetingId={}, userId={}", meetingId, userId);
            removeEmitter(meetingId, userId, emitter);
        });
        emitter.onError(exception -> {
            log.debug("Transcript SSE emitter error callback: meetingId={}, userId={}", meetingId, userId, exception);
            removeEmitter(meetingId, userId, emitter);
        });

        return emitter;
    }

    public void broadcastSegment(
            String meetingId,
            String uploaderUserId,
            MeetingTranscriptSegmentResponse response
    ) {
        String segmentId = segmentId(response);
        ConcurrentMap<String, SseEmitter> meetingEmitters = emitters.get(meetingId);
        if (meetingEmitters == null || meetingEmitters.isEmpty()) {
            log.debug(
                    "Transcript SSE broadcast skipped, no subscribers: meetingId={}, uploaderUserId={}, segmentId={}",
                    meetingId,
                    uploaderUserId,
                    segmentId
            );
            return;
        }

        long targetSubscriberCount = meetingEmitters.keySet().stream()
                .filter(userId -> !userId.equals(uploaderUserId))
                .count();
        log.debug(
                "Transcript SSE broadcast started: meetingId={}, uploaderUserId={}, segmentId={}, subscribers={}, targets={}",
                meetingId,
                uploaderUserId,
                segmentId,
                meetingEmitters.size(),
                targetSubscriberCount
        );

        meetingEmitters.forEach((userId, emitter) -> {
            if (userId.equals(uploaderUserId)) {
                log.debug(
                        "Transcript SSE broadcast skipped uploader: meetingId={}, userId={}, segmentId={}",
                        meetingId,
                        userId,
                        segmentId
                );
                return;
            }
            sendSegment(meetingId, userId, emitter, response);
        });
    }

    public void disconnectUser(String meetingId, String userId) {
        log.debug("Transcript SSE disconnect requested: meetingId={}, userId={}", meetingId, userId);
        ConcurrentMap<String, SseEmitter> meetingEmitters = emitters.get(meetingId);
        if (meetingEmitters == null) {
            log.debug("Transcript SSE disconnect skipped, meeting has no subscribers: meetingId={}, userId={}",
                    meetingId, userId);
            return;
        }

        SseEmitter emitter = meetingEmitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.debug(
                    "Transcript SSE emitter disconnected: meetingId={}, userId={}, subscriberCount={}",
                    meetingId,
                    userId,
                    meetingEmitters.size()
            );
        } else {
            log.debug("Transcript SSE disconnect skipped, user has no emitter: meetingId={}, userId={}",
                    meetingId, userId);
        }
        if (meetingEmitters.isEmpty()) {
            emitters.remove(meetingId, meetingEmitters);
            log.debug("Transcript SSE meeting emitter bucket removed: meetingId={}", meetingId);
        }
    }

    int subscriberCount(String meetingId) {
        ConcurrentMap<String, SseEmitter> meetingEmitters = emitters.get(meetingId);
        if (meetingEmitters == null) {
            log.debug("Transcript SSE subscriber count requested: meetingId={}, subscriberCount=0", meetingId);
            return 0;
        }
        log.debug("Transcript SSE subscriber count requested: meetingId={}, subscriberCount={}",
                meetingId, meetingEmitters.size());
        return meetingEmitters.size();
    }

    private void sendSegment(
            String meetingId,
            String userId,
            SseEmitter emitter,
            MeetingTranscriptSegmentResponse response
    ) {
        String segmentId = segmentId(response);
        try {
            emitter.send(SseEmitter.event()
                    .name(TRANSCRIPT_SEGMENT_CREATED_EVENT)
                    .data(response));
            log.debug("Transcript SSE segment sent: meetingId={}, userId={}, segmentId={}",
                    meetingId, userId, segmentId);
        } catch (IOException | IllegalStateException exception) {
            log.debug("Transcript SSE send failed: meetingId={}, userId={}, segmentId={}",
                    meetingId, userId, segmentId, exception);
            removeEmitter(meetingId, userId, emitter);
        }
    }

    private void removeEmitter(String meetingId, String userId, SseEmitter emitter) {
        ConcurrentMap<String, SseEmitter> meetingEmitters = emitters.get(meetingId);
        if (meetingEmitters == null) {
            log.debug("Transcript SSE cleanup skipped, meeting has no subscribers: meetingId={}, userId={}",
                    meetingId, userId);
            return;
        }

        boolean removed = meetingEmitters.remove(userId, emitter);
        log.debug(
                "Transcript SSE cleanup finished: meetingId={}, userId={}, removed={}, subscriberCount={}",
                meetingId,
                userId,
                removed,
                meetingEmitters.size()
        );
        if (meetingEmitters.isEmpty()) {
            emitters.remove(meetingId, meetingEmitters);
            log.debug("Transcript SSE meeting emitter bucket removed during cleanup: meetingId={}", meetingId);
        }
    }

    private String segmentId(MeetingTranscriptSegmentResponse response) {
        if (response == null) {
            return null;
        }
        return response.getSegmentId();
    }
}
