package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.MeetingTranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingTranscriptSegmentRepository extends JpaRepository<MeetingTranscriptSegment, String> {
    Optional<MeetingTranscriptSegment> findByMeeting_IdAndParticipant_IdAndSegmentId(
            String meetingId,
            String participantId,
            String segmentId
    );
}
