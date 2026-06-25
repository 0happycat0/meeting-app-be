package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.response.MeetingMinutesResponse;
import com.happycat.meetingappbe.entity.MeetingMinutes;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MeetingMinutesMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    @Mapping(target = "generatedById", source = "generatedBy.id")
    MeetingMinutesResponse toResponse(MeetingMinutes minutes);
}
