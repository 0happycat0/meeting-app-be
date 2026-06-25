package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.MeetingTranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingTranscriptSegmentRepository extends JpaRepository<MeetingTranscriptSegment, String> {
    Optional<MeetingTranscriptSegment> findByMeeting_IdAndParticipant_IdAndSegmentId(
            String meetingId,
            String participantId,
            String segmentId
    );

    @Query("""
            select s
            from MeetingTranscriptSegment s
            join fetch s.user
            join fetch s.participant
            where s.meeting.id = :meetingId
            order by coalesce(s.clientCreatedAt, s.createdAt) asc, s.createdAt asc
            """)
    List<MeetingTranscriptSegment> findForMinutesByMeetingId(@Param("meetingId") String meetingId);
}
