package com.boardroom.backend.service;

import com.boardroom.backend.dto.AuthResponse;
import com.boardroom.backend.dto.LoginRequest;
import com.boardroom.backend.dto.RegisterRequest;
import com.boardroom.backend.dto.UserDto;
import com.boardroom.backend.model.User;
import com.boardroom.backend.repository.UserRepository;
import com.boardroom.backend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        String trimmedUsername = request.getUsername().trim();
        String trimmedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = new User();
        user.setUsername(trimmedUsername);
        user.setEmail(trimmedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
        UserDto userDto = new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());

        return new AuthResponse(token, userDto, "User registered successfully");
    }

    public AuthResponse login(LoginRequest request) {
        String loginIdentifier = request.getUsernameOrEmail().trim();

        User user = userRepository.findByUsernameOrEmail(loginIdentifier, loginIdentifier.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username/email or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getEmail());
        UserDto userDto = new UserDto(user.getId(), user.getUsername(), user.getEmail());

        return new AuthResponse(token, userDto, "Login successful");
    }

    public UserDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserDto(user.getId(), user.getUsername(), user.getEmail());
    }
}
