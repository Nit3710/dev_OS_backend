package com.devos.core.service;

import com.devos.core.dto.AuthResponse;
import com.devos.core.domain.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;

public interface AuthService {

    AuthResponse authenticate(String username, String password);

    AuthResponse refreshToken(String refreshToken);

    void logout(String token);

    User getCurrentUser(String token);

    String extractUsername(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

    String generateToken(User user);

    String generateRefreshToken(User user);

    Date extractExpiration(String token);

    boolean isTokenExpired(String token);
}
