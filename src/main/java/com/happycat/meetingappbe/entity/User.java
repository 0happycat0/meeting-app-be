package com.happycat.meetingappbe.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users",
        uniqueConstraints =  {
            @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        })
public class User {
    @Id
    String id;
    String username;
    String firstName;
    String lastName;
    String email;
    LocalDate dob;
}
