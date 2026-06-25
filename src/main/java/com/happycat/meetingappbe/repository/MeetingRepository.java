package com.happycat.meetingappbe.repository;

import com.happycat.meetingappbe.entity.Meeting;
import com.happycat.meetingappbe.enums.MeetingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, String> {
    boolean existsByJoinCode(String joinCode);

    Optional<Meeting> findByJoinCode(String joinCode);

    @Query("""
            select m
            from Meeting m
            join fetch m.host
            where m.status = :status
            order by
              case when m.scheduledStartAt is null then 1 else 0 end,
              m.scheduledStartAt asc,
              m.createdAt desc
            """)
    List<Meeting> findByStatusWithHost(@Param("status") MeetingStatus status);
}
