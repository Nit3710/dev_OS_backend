package com.devos.core.service.impl;

import com.devos.core.dto.AuthResponse;
import com.devos.core.dto.UserDto;
import com.devos.core.domain.entity.User;
import com.devos.core.exception.DevosException;
import com.devos.core.repository.UserRepository;
import com.devos.core.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${devos.security.jwt.secret}")
    private String secret;

    @Value("${devos.security.jwt.expiration}")
    private Long jwtExpiration;

    @Value("${devos.security.jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Override
    public AuthResponse authenticate(String username, String password) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.getEnabled()) {
            throw new DevosException("User account is disabled");
        }

        String token = generateToken(user);
        String refreshToken = generateRefreshToken(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .user(UserDto.from(user))
                .build();
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            String username = extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            if (!isTokenExpired(refreshToken)) {
                String newToken = generateToken(user);
                String newRefreshToken = generateRefreshToken(user);

                return AuthResponse.builder()
                        .token(newToken)
                        .refreshToken(newRefreshToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtExpiration)
                        .user(UserDto.from(user))
                        .build();
            } else {
                throw new RuntimeException("Refresh token expired");
            }
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            throw new RuntimeException("Invalid refresh token", e);
        }
    }

    @Override
    public void logout(String token) {
        // In a production environment, you would add the token to a blacklist
        // For now, we'll just log the logout
        log.info("User logged out with token: {}", token.substring(0, Math.min(10, token.length())));
    }

    @Override
    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Override
    public User getCurrentUser(String token) {
        try {
            String username = extractUsername(token);
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        } catch (Exception e) {
            log.error("Error getting current user from token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }

    @Override
    public String getCurrentToken() {
        // This assumes the token is stored in the credentials or we can extract it if needs be.
        // Actually, usually in JWT setup, the token itself isn't stored in Authentication object 
        // unless we explicitly put it there during filter. 
        // For now, let's return a placeholder or handle it if we strictly need the raw token in services.
        // If we only need User, getCurrentUser() is better.
        return null; 
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    @Override
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        claims.put("email", user.getEmail());
        
        return createToken(claims, String.valueOf(user.getId()), jwtExpiration);
    }

    @Override
    public String generateRefreshToken(User user) {
        return createToken(new HashMap<>(), String.valueOf(user.getId()), refreshExpiration);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    @SuppressWarnings("deprecation")
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, getSignInKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    @SuppressWarnings("deprecation")
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(getSignInKey())
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
