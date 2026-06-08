package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.MeetingInvitation;
import com.happycat.meetingappbe.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MeetingInvitationRepository extends JpaRepository<MeetingInvitation, String> {
    List<MeetingInvitation> findByMeeting_IdOrderBySentAtDesc(String meetingId);

    List<MeetingInvitation> findByInvitee_IdOrderBySentAtDesc(String inviteeId);

    boolean existsByMeeting_IdAndInvitee_IdAndStatusIn(
            String meetingId,
            String inviteeId,
            Collection<InvitationStatus> statuses
    );

    boolean existsByMeeting_IdAndInvitee_IdAndStatus(
            String meetingId,
            String inviteeId,
            InvitationStatus status
    );

    Optional<MeetingInvitation> findFirstByMeeting_IdAndInvitee_IdAndStatusInOrderBySentAtDesc(
            String meetingId,
            String inviteeId,
            Collection<InvitationStatus> statuses
    );
}
