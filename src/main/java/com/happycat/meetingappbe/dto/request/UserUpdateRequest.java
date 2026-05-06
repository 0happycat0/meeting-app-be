package com.happycat.meetingappbe.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequest {
    @Size(min = 6, message = "PASSWORD_INVALID")
    String password;

    String firstName;
    String lastName;

    @NotBlank(message = "EMAIL_INVALID")
    @Email(message = "EMAIL_INVALID")
    String email;
    LocalDate dob;
}
