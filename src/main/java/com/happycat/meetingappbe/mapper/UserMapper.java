package com.happycat.meetingappbe.mapper;

import com.happycat.meetingappbe.dto.request.UserCreationRequest;
import com.happycat.meetingappbe.dto.request.UserUpdateRequest;
import com.happycat.meetingappbe.dto.response.UserResponse;
import com.happycat.meetingappbe.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);

    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
