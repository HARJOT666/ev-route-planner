package com.example.evrouteplanner.service;

import com.example.evrouteplanner.dto.AuthResponse;
import com.example.evrouteplanner.dto.LoginRequest;
import com.example.evrouteplanner.dto.RegisterRequest;
import com.example.evrouteplanner.exception.BadRequestException;
import com.example.evrouteplanner.model.User;
import com.example.evrouteplanner.repository.UserRepository;
import com.example.evrouteplanner.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles registration and login.
 *
 * Register: hash the password, save the user, return a JWT.
 * Login:    verify email + password, then return a JWT.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // store a BCrypt hash
        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        // Let Spring Security check the email/password against the stored hash.
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BadRequestException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}
