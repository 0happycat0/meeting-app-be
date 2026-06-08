package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.MeetingTokenLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingTokenLogRepository extends JpaRepository<MeetingTokenLog, String> {
}
