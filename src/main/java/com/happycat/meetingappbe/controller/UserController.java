package com.happycat.meetingappbe.controller;

import com.happycat.meetingappbe.dto.ApiResponse;
import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.UserCreationRequest;
import com.happycat.meetingappbe.dto.request.UserUpdateRequest;
import com.happycat.meetingappbe.dto.response.UserResponse;
import com.happycat.meetingappbe.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('PERM_client_admin', 'PERM_client_user')")
    ApiResponse<PageResponse<UserResponse>> getUsers() {
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_client_admin')")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('PERM_client_admin')")
    UserResponse updateUser(@PathVariable String userId, @RequestBody @Valid UserUpdateRequest request) {
        return userService.updateUser(userId, request);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('PERM_client_admin')")
    ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User has been deleted")
                .build();
    }
}
