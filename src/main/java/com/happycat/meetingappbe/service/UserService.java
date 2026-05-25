package com.happycat.meetingappbe.service;

import com.happycat.meetingappbe.dto.PageResponse;
import com.happycat.meetingappbe.dto.request.UserCreationRequest;
import com.happycat.meetingappbe.dto.request.UserUpdateRequest;
import com.happycat.meetingappbe.dto.response.UserResponse;
import com.happycat.meetingappbe.entity.User;
import com.happycat.meetingappbe.exception.AppException;
import com.happycat.meetingappbe.exception.ErrorCode;
import com.happycat.meetingappbe.mapper.UserMapper;
import com.happycat.meetingappbe.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;

    Keycloak keycloak;

    @NonFinal
    @Value("${keycloak.admin.realm}")
    String realm;

    public PageResponse<UserResponse> getUsers() {
        return PageResponse.<UserResponse>builder()
                .items(userRepository.findAll().stream().map(userMapper::toUserResponse).toList())
                .total(userRepository.count())
                .build();
    }

//    public List<UserRepresentation> getAllUsers() {
//        return keycloak.realm(realm).users().list();
//    }

    // Cập nhâtk Keycloak trước -> cập nhật db
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            log.error("User {} existed", request.getUsername());
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(request.getUsername());
        userRep.setEmail(request.getEmail());
        userRep.setFirstName(request.getFirstName());
        userRep.setLastName(request.getLastName());
        userRep.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false); // Pass không phải tạm thời
        userRep.setCredentials(List.of(credential));

        // Request đến Keycloak
        Response response = keycloak.realm(realm).users().create(userRep);

        if (response.getStatus() == 409) {
            log.warn("User {} đã tồn tại trên Keycloak", request.getUsername());
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        if (response.getStatus() != 201) {
            log.error("Lỗi khi tạo user trên Keycloak, Status: {}, Body: {}",
                    response.getStatus(),
                    response.readEntity(String.class));
            throw new RuntimeException("Không thể tạo user trên hệ thống định danh");
        }

        // Get UUID
        String keycloakUserId = CreatedResponseUtil.getCreatedId(response);

        try {
            User user = userMapper.toUser(request);
            user.setId(keycloakUserId); // Sử dụng Keycloak ID

            user = userRepository.save(user);
            return userMapper.toUserResponse(user);

        } catch (Exception e) {
            log.error("Lỗi khi lưu DB, tiến hành xóa user {} trên Keycloak", keycloakUserId);
            keycloak.realm(realm).users().get(keycloakUserId).remove();
            throw new RuntimeException("Lỗi save user, đã rollback dữ liệu", e);
        }
    }

    @Transactional
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        UserResource userResource = keycloak.realm(realm).users().get(id);
        UserRepresentation userRep = userResource.toRepresentation();

        userRep.setFirstName(request.getFirstName());
        userRep.setLastName(request.getLastName());
        userRep.setEmail(request.getEmail());
        userResource.update(userRep);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(request.getPassword());
            credential.setTemporary(false);
            userResource.resetPassword(credential);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteUser(String id) {
        keycloak.realm(realm).users().get(id).remove();
        userRepository.deleteById(id);
    }
}
