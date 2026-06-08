package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.entity.MeetingParticipant;
import com.happycat.meetingappbe.enums.InvitationStatus;
import com.happycat.meetingappbe.enums.MeetingStatus;
import com.happycat.meetingappbe.enums.MeetingType;
import com.happycat.meetingappbe.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, String> {
    Optional<MeetingParticipant> findByMeeting_IdAndUser_Id(String meetingId, String userId);

    Optional<MeetingParticipant> findByIdAndMeeting_Id(String participantId, String meetingId);

    List<MeetingParticipant> findByMeeting_IdOrderByCreatedAtAsc(String meetingId);

    List<MeetingParticipant> findByMeeting_IdAndParticipationStatusOrderByRequestedAtAsc(
            String meetingId,
            ParticipationStatus participationStatus
    );

    @Query("""
            select p
            from MeetingParticipant p
            join fetch p.meeting m
            join fetch m.host
            where p.user.id = :userId
              and (
                p.participationStatus <> :invitedParticipationStatus
                or exists (
                  select 1
                  from MeetingInvitation i
                  where i.meeting = m
                    and i.invitee.id = :userId
                    and i.status = :acceptedInvitationStatus
                )
              )
              and (:status is null or m.status = :status)
              and (:type is null or m.meetingType = :type)
            order by
              case when m.scheduledStartAt is null then 1 else 0 end,
              m.scheduledStartAt asc,
              m.createdAt desc
            """)
    List<MeetingParticipant> findMyMeetings(
            @Param("userId") String userId,
            @Param("status") MeetingStatus status,
            @Param("type") MeetingType type,
            @Param("invitedParticipationStatus") ParticipationStatus invitedParticipationStatus,
            @Param("acceptedInvitationStatus") InvitationStatus acceptedInvitationStatus
    );

    boolean existsByMeetingAndUser_Id(Meeting meeting, String userId);
}
