package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.response.MeetingParticipantResponse;
import com.happycat.meetingappbe.entity.MeetingParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MeetingParticipantMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "approvedById", source = "approvedBy.id")
    @Mapping(target = "removedById", source = "removedBy.id")
    MeetingParticipantResponse toResponse(MeetingParticipant participant);
}
