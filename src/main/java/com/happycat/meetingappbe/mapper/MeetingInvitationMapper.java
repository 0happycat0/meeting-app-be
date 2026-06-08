package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.response.MeetingInvitationResponse;
import com.happycat.meetingappbe.entity.MeetingInvitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MeetingInvitationMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    @Mapping(target = "meetingTitle", source = "meeting.title")
    @Mapping(target = "meetingType", source = "meeting.meetingType")
    @Mapping(target = "meetingStatus", source = "meeting.status")
    @Mapping(target = "scheduledStartAt", source = "meeting.scheduledStartAt")
    @Mapping(target = "scheduledEndAt", source = "meeting.scheduledEndAt")
    @Mapping(target = "inviterId", source = "inviter.id")
    @Mapping(target = "inviterFirstName", source = "inviter.firstName")
    @Mapping(target = "inviterLastName", source = "inviter.lastName")
    @Mapping(target = "inviteeId", source = "invitee.id")
    MeetingInvitationResponse toResponse(MeetingInvitation invitation);
}
