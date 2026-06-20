package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.response.MeetingTranscriptSegmentResponse;
import com.happycat.meetingappbe.entity.MeetingTranscriptSegment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MeetingTranscriptSegmentMapper {
    @Mapping(target = "meetingId", source = "meeting.id")
    @Mapping(target = "participantId", source = "participant.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    MeetingTranscriptSegmentResponse toResponse(MeetingTranscriptSegment segment);
}
