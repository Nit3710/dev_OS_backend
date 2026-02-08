package com.devos.api.controller;

import com.devos.api.dto.AuthRequest;
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
@RequestMapping("/auth")
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

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserDto userDto) {
        log.info("Registration attempt for user: {}", userDto.getUsername());
        
        User user = userService.createUser(userDto);
        
        log.info("Registration successful for user: {}", user.getUsername());
        return ResponseEntity.ok(UserDto.from(user));
    }
}
