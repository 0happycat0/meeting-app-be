package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.response.JoinMeetingResponse;
import com.happycat.meetingappbe.dto.response.MeetingResponse;
import com.happycat.meetingappbe.entity.Meeting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface MeetingMapper {
    @Mapping(target = "hostId", source = "host.id")
    @Mapping(target = "hostFirstName", source = "host.firstName")
    @Mapping(target = "hostLastName", source = "host.lastName")
    @Mapping(target = "joinCode", source = "joinCode", qualifiedByName = "formatJoinCode")
    MeetingResponse toMeetingResponse(Meeting meeting);

    @Mapping(target = "hostId", source = "host.id")
    @Mapping(target = "hostFirstName", source = "host.firstName")
    @Mapping(target = "hostLastName", source = "host.lastName")
    @Mapping(target = "joinCode", source = "joinCode", qualifiedByName = "formatJoinCode")
    JoinMeetingResponse toJoinMeetingResponse(Meeting meeting);

    @Named("formatJoinCode")
    default String formatJoinCode(String joinCode) {
        if (joinCode == null || joinCode.length() != 8) {
            return joinCode;
        }
        return joinCode.substring(0, 4) + "-" + joinCode.substring(4);
    }
}
