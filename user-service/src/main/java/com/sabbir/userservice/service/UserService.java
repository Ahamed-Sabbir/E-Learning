package com.sabbir.userservice.service;

import com.sabbir.userservice.model.User;
import com.sabbir.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.sabbir.userservice.constant.UserServiceConstant.VALID_EMAIL_REGEX;
import static com.sabbir.userservice.constant.UserServiceConstant.VALID_PASSWORD_REGEX;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(UUID userId) {
        return userRepository.findByUserId(userId);
    }

    public List<User> getListOfUser(List<UUID> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public void addUser(User user) throws Exception {
        User existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser != null) {
            throw new Exception("User with email id - " + user.getEmail() + " already exists!");
        }
        userRepository.save(user);
    }

    /**
     * Updates the user's information.
     *
     * @param user user data with updated information.
     *             username, password, role will be handled by keycloak
     * @throws Exception if the user is not found.
     */
    public void updateUser(User user) throws Exception {
        User existingUser = userRepository.findByUserId(user.getUserId());
        if (existingUser == null) {
            throw new Exception("User with id - " + user.getUserId() + " not found!");
        }
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setGender(user.getGender());
        existingUser.setDateOfBirth(user.getDateOfBirth());
        userRepository.save(existingUser);
    }

    /**
     * Updates the user's profile image url.
     *
     * @param userId   UUID of the user for whom to update the profile image url.
     * @param imageUrl new image url.
     * @throws Exception if the user is not found or an error occurs during the image update.
     */
    public void updateProfileImageUrl(UUID userId, String imageUrl) throws Exception {
        User existingUser = userRepository.findByUserId(userId);
        if (existingUser == null) {
            throw new Exception("User with id - " + userId + " not found!");
        }
        existingUser.setImageUrl(imageUrl);
        userRepository.save(existingUser);
    }

    public void validateEmail(String email) throws Exception {
        boolean isEmailValid = isEmailValid(email);
        if (!isEmailValid) {
            throw new Exception("Email id - " + email + " is not valid!");
        }
    }

    public void validatePassword(String password) throws Exception {
        boolean isPasswordValid = isPasswordValid(password);
        if (!isPasswordValid) {
            throw new Exception("Password should be minimum eight characters. And contain at least one uppercase " +
                    "letter, one lowercase letter, one number and one special character!");
        }
    }

    private boolean isEmailValid(String email) {
        if (!StringUtils.hasLength(email)) {
            return false;
        }
        return Pattern.compile(VALID_EMAIL_REGEX).matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        if (!StringUtils.hasLength(password)) {
            return false;
        }
        return Pattern.compile(VALID_PASSWORD_REGEX).matcher(password).matches();
    }
}
