package com.devos.core.service.impl;

import com.devos.core.dto.UserDto;
import com.devos.core.domain.entity.User;
import com.devos.core.domain.entity.UserSettings;
import com.devos.core.repository.UserRepository;
import com.devos.core.service.AuthService;
import com.devos.core.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserServiceImpl(
            UserRepository userRepository,
            @Lazy PasswordEncoder passwordEncoder,
            @Lazy AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Override
    @Transactional
    public User createUser(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(userDto.getUsername())
                .email(userDto.getEmail())
                .passwordHash(passwordEncoder.encode("defaultPassword123")) // Should be sent in request
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .avatarUrl(userDto.getAvatarUrl())
                .role(userDto.getRole() != null ? userDto.getRole() : User.UserRole.DEVELOPER)
                .enabled(true)
                .emailVerified(false)
                .build();

        // Create default settings
        UserSettings settings = UserSettings.builder()
                .user(user)
                .build();
        user.setSettings(settings);

        User savedUser = userRepository.save(user);
        log.info("Created user: {}", savedUser.getUsername());
        
        return savedUser;
    }

    @Override
    @Transactional
    public User updateUser(String token, UserDto userDto) {
        User currentUser = getUserFromToken(token);
        
        currentUser.setFirstName(userDto.getFirstName());
        currentUser.setLastName(userDto.getLastName());
        currentUser.setAvatarUrl(userDto.getAvatarUrl());
        
        // Only allow role changes for admins
        if (currentUser.getRole() == User.UserRole.ADMIN && userDto.getRole() != null) {
            currentUser.setRole(userDto.getRole());
        }
        
        User updatedUser = userRepository.save(currentUser);
        log.info("Updated user: {}", updatedUser.getUsername());
        
        return updatedUser;
    }

    @Override
    @Transactional
    public UserSettings updateUserSettings(User user, UserSettings settings) {
        UserSettings currentSettings = user.getSettings();
        
        currentSettings.setTheme(settings.getTheme());
        currentSettings.setLanguage(settings.getLanguage());
        currentSettings.setTimezone(settings.getTimezone());
        currentSettings.setNotificationsEnabled(settings.getNotificationsEnabled());
        currentSettings.setAutoSaveEnabled(settings.getAutoSaveEnabled());
        currentSettings.setCodeFontSize(settings.getCodeFontSize());
        currentSettings.setTabSize(settings.getTabSize());
        currentSettings.setWordWrap(settings.getWordWrap());
        currentSettings.setMinimapEnabled(settings.getMinimapEnabled());
        currentSettings.setCustomPreferences(settings.getCustomPreferences());
        currentSettings.setDefaultLlmProvider(settings.getDefaultLlmProvider());
        currentSettings.setMaxTokensPerRequest(settings.getMaxTokensPerRequest());
        currentSettings.setAutoIndexProjects(settings.getAutoIndexProjects());
        
        return userRepository.save(user).getSettings();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public User getUserFromToken(String token) {
        return authService.getCurrentUser(token);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    @Override
    public boolean isCurrentUser(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String username = authentication.getName();
        try {
            Long currentUserId = Long.parseLong(username);
            return currentUserId.equals(userId);
        } catch (NumberFormatException e) {
            // If username is not a number, try to find user by username
            User user = userRepository.findByUsername(username).orElse(null);
            return user != null && user.getId().equals(userId);
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        User user = getUserFromToken(token);
        return user.getId();
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(String.valueOf(user.getId())) // Use user ID as username for security context
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(!user.getEnabled())
                .credentialsExpired(false)
                .disabled(!user.getEnabled())
                .build();
    }
}
