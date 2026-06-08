package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    boolean existsByJoinCode(String joinCode);

    Optional<Meeting> findByJoinCode(String joinCode);
}
