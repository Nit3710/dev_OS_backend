package com.devos.api.controller;

import com.devos.api.dto.AuthRequest;
import com.devos.api.dto.RegisterRequest;
import com.devos.core.dto.AuthResponse;
import com.devos.core.dto.UserDto;
import com.devos.core.domain.entity.User;
import com.devos.core.service.AuthService;
import com.devos.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        log.info("Login attempt for user: {}", authRequest.getUsername());
        
        AuthResponse response = authService.authenticate(authRequest.getUsername(), authRequest.getPassword());
        
        log.info("Login successful for user: {}", authRequest.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration attempt for user: {}", registerRequest.getUsername());
        
        // Create user
        UserDto userDto = new UserDto();
        userDto.setUsername(registerRequest.getUsername());
        userDto.setEmail(registerRequest.getEmail());
        userDto.setFirstName(registerRequest.getFirstName());
        userDto.setLastName(registerRequest.getLastName());
        userDto.setPassword(registerRequest.getPassword()); // Set password for user creation
        
        User user = userService.createUser(userDto);
        
        // Authenticate and return tokens
        AuthResponse response = authService.authenticate(registerRequest.getUsername(), registerRequest.getPassword());
        
        log.info("Registration successful for user: {}", user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestParam String refreshToken) {
        log.info("Token refresh request");
        
        AuthResponse response = authService.refreshToken(refreshToken);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        log.info("Logout request");
        
        String jwtToken = token.replace("Bearer ", "");
        authService.logout(jwtToken);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@RequestHeader("Authorization") String token) {
        String jwtToken = token.replace("Bearer ", "");
        User user = authService.getCurrentUser(jwtToken);
        
        return ResponseEntity.ok(UserDto.from(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(@RequestHeader("Authorization") String token, 
                                               @Valid @RequestBody UserDto userDto) {
        String jwtToken = token.replace("Bearer ", "");
        User currentUser = authService.getCurrentUser(jwtToken);
        
        userDto.setId(currentUser.getId());
        User updatedUser = userService.updateUser(userDto);
        
        return ResponseEntity.ok(UserDto.from(updatedUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestHeader("Authorization") String token,
                                             @RequestParam String oldPassword,
                                             @RequestParam String newPassword) {
        String jwtToken = token.replace("Bearer ", "");
        User currentUser = authService.getCurrentUser(jwtToken);
        
        userService.changePassword(currentUser.getId(), oldPassword, newPassword);
        
        return ResponseEntity.ok().build();
    }
}
