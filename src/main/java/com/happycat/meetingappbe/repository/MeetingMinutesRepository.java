package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.MeetingMinutes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, String> {
    Optional<MeetingMinutes> findByMeeting_Id(String meetingId);

    List<MeetingMinutes> findByMeeting_IdIn(Collection<String> meetingIds);
}
