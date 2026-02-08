package com.devos.api.controller;

import com.devos.core.dto.UserDto;
import com.devos.core.domain.entity.User;
import com.devos.core.domain.entity.UserSettings;
import com.devos.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<UserDto> getProfile(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        User user = userService.getUserFromToken(jwtToken);
        
        return ResponseEntity.ok(UserDto.from(user));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<UserDto> updateProfile(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserDto userDto) {
        
        String jwtToken = token.replace("Bearer ", "");
        User user = userService.updateUser(jwtToken, userDto);
        
        log.info("Profile updated for user: {}", user.getUsername());
        return ResponseEntity.ok(UserDto.from(user));
    }

    @GetMapping("/settings")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<UserSettings> getSettings(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        User user = userService.getUserFromToken(jwtToken);
        
        return ResponseEntity.ok(user.getSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('DEVELOPER') or hasRole('ADMIN')")
    public ResponseEntity<UserSettings> updateSettings(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody UserSettings settings) {
        
        String jwtToken = token.replace("Bearer ", "");
        User user = userService.getUserFromToken(jwtToken);
        UserSettings updatedSettings = userService.updateUserSettings(user, settings);
        
        log.info("Settings updated for user: {}", user.getUsername());
        return ResponseEntity.ok(updatedSettings);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDto> userDtos = users.stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userService.isCurrentUser(#id, authentication)")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(UserDto.from(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        log.info("User deleted with ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
