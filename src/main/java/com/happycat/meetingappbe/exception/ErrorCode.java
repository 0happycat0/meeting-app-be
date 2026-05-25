package com.happycat.meetingappbe.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    // 10xx: system error
    // 20xx: auth error
    // 30xx: user error
    // 40xx: meeting error
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(2001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(2002, "You do not have permission", HttpStatus.FORBIDDEN),

    USER_EXISTED(3001, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_EXISTED(3002, "User not existed", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(3003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(3004, "Password must be at least 6 characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(3005, "Email format is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(3006, "Email already exists", HttpStatus.CONFLICT),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    int code;
    String message;
    HttpStatusCode statusCode;

}
