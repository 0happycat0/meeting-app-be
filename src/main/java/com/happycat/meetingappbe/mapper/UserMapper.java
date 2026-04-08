package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.response.UserResponse;
import com.happycat.meetingappbe.entity.User;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
