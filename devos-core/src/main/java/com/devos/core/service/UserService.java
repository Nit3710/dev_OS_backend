package com.devos.core.service;

import com.devos.core.dto.UserDto;
import com.devos.core.domain.entity.User;
import com.devos.core.domain.entity.UserSettings;

import java.util.List;

public interface UserService extends org.springframework.security.core.userdetails.UserDetailsService {

    User createUser(UserDto userDto);

    User updateUser(String token, UserDto userDto);

    UserSettings updateUserSettings(User user, UserSettings settings);

    List<User> getAllUsers();

    User getUserById(Long id);

    User getUserFromToken(String token);

    void deleteUser(Long id);

    boolean isCurrentUser(Long userId, org.springframework.security.core.Authentication authentication);

    Long getUserIdFromToken(String token);
}
