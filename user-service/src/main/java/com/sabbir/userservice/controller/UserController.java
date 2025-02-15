package com.sabbir.userservice.controller;

import com.sabbir.commonmodule.model.CustomHttpResponse;
import com.sabbir.commonmodule.util.ResponseBuilder;
import com.sabbir.userservice.model.User;
import com.sabbir.userservice.service.KeycloakService;
import com.sabbir.userservice.service.UserService;
import org.keycloak.admin.client.resource.UserResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.sabbir.commonmodule.constant.CommonConstant.MESSAGE;
import static com.sabbir.userservice.constant.UserServiceConstant.ROLE_USER;
import static com.sabbir.userservice.constant.UserServiceConstant.ROLE_ADMIN;

@RestController
@RequestMapping("/user")
public class UserController {
    private final KeycloakService keycloakService;
    private final UserService userService;

    public UserController(KeycloakService keycloakService, UserService userService) {
        this.keycloakService = keycloakService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomHttpResponse> addRegularUser(@RequestBody User user) {
        try {
            userService.validateEmail(user.getEmail());
            userService.validatePassword(user.getPassword());
            user.setRole(ROLE_USER);
            UUID userId = keycloakService.registerNewUser(user);
            user.setUserId(userId);
            userService.addUser(user);
        } catch (Exception ex) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.BAD_REQUEST, 400,
                    "Failed to add user! Reason: " + ex.getMessage());
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.CREATED, Map.of(MESSAGE, "Successfully added user"));
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomHttpResponse> addAdminUser(@RequestBody User user) {
        try {
            userService.validateEmail(user.getEmail());
            userService.validatePassword(user.getPassword());
            user.setRole(ROLE_ADMIN);
            UUID userId = keycloakService.registerNewUser(user);
            user.setUserId(userId);
            userService.addUser(user);
        } catch (Exception ex) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.BAD_REQUEST, 400,
                    "Failed to add admin user! Reason: " + ex.getMessage());
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.CREATED, Map.of(MESSAGE, "Successfully added admin"));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomHttpResponse> getUserById(@PathVariable UUID userId) {
        User user = userService.getUser(userId);
        if (user == null) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.NOT_FOUND, 404,
                    "No user found for this user id!");
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of("user", user));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<CustomHttpResponse> getAllRegularUser() {
        List<UUID> userIds = keycloakService.getUserIdsByRole(ROLE_USER);
        List<User> userList = userService.getListOfUser(userIds);
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of("userList", userList));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomHttpResponse> getAllAdminUser() {
        List<UUID> userIds = keycloakService.getUserIdsByRole(ROLE_ADMIN);
        List<User> userList = userService.getListOfUser(userIds);
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of("userList", userList));
    }

    @PostMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomHttpResponse> getListOfUser(@RequestBody Map<String, List<UUID>> userIdsMap) {
        List<User> userList;
        try {
            userList = userService.getListOfUser(userIdsMap.get("userIds"));
        } catch (Exception ex) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.BAD_REQUEST, 400,
                    "Failed to fetch user information! Reason: " + ex.getMessage());
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of("userList", userList));
    }

    /**
     * API to update user profile information.
     * <p>
     * To use this API, client application needs to pass access token of that particular user.
     * Or it needs to pass access token with role 'ADMIN'.
     *  jwt token contains userId by subject key --> authentication.principal.subject
     * @param user request payload containing user profile information.
     * @return success if operation is successful. Else returns 417-Expectation Failed.
     */
    @PostMapping("/profile")
    @PreAuthorize("hasRole('ADMIN') or #user.userId.toString() == authentication.principal.subject")
    public ResponseEntity<CustomHttpResponse> updateProfile(@RequestBody User user) {
        try {
            keycloakService.updateUser(user);
            userService.updateUser(user);
        } catch (Exception ex) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.EXPECTATION_FAILED, 417,
                    "Failed to update user information! Reason: " + ex.getMessage());
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of(MESSAGE,
                "Successfully updated user information"));
    }

    /**
     * API to update user profile picture url.
     * <p>
     * To use this API, client application needs to pass access token of that particular user.
     * Or it needs to pass access token with role 'ADMIN'.
     *
     * @param imageUrlMap map containing user id and image url data.
     * @return success if operation is successful. Else returns 417-Expectation Failed.
     */
    @PostMapping("/image")
    @PreAuthorize("hasRole('ADMIN') or #imageUrlMap.get('userId') == authentication.principal.subject")
    public ResponseEntity<CustomHttpResponse> updateImageUrl(@RequestBody Map<String, String> imageUrlMap) {
        try {
            userService.updateProfileImageUrl(UUID.fromString(imageUrlMap.get("userId")), imageUrlMap.get("imageUrl"));
        } catch (Exception ex) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.EXPECTATION_FAILED, 417,
                    "Failed to update profile image! Reason: " + ex.getMessage());
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of(MESSAGE,
                "Successfully updated profile image"));
    }

    /**
     * API to update account password.
     * <p>
     * To use this API, client application needs to pass access token of that particular user.
     * Or it needs to pass access token with role 'ADMIN'.
     *
     * @param passwordMap map containing user id and new password.
     * @return success if operation is successful. Else returns 417-Expectation Failed.
     */
    @PostMapping("/password")
    @PreAuthorize("hasRole('ADMIN') or #passwordMap.get('userId') == authentication.principal.subject")
    public ResponseEntity<CustomHttpResponse> updatePassword(@RequestBody Map<String, String> passwordMap) {
        try {
            String userId = passwordMap.get("userId");
            String password = passwordMap.get("password");
            userService.validatePassword(password);
            UserResource userResource = keycloakService.getUserResourceById(userId);
            keycloakService.updateUserCredentials(userResource, password);
        } catch (Exception ex) {
            return ResponseBuilder.buildFailureResponse(HttpStatus.EXPECTATION_FAILED, 417,
                    "Failed to update password! Reason: " + ex.getMessage());
        }
        return ResponseBuilder.buildSuccessResponse(HttpStatus.OK, Map.of(MESSAGE,
                "Successfully updated password"));
    }
}
