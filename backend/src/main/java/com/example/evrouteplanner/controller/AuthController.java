package com.example.evrouteplanner.controller;

import com.example.evrouteplanner.dto.AuthResponse;
import com.example.evrouteplanner.dto.LoginRequest;
import com.example.evrouteplanner.dto.RegisterRequest;
import com.example.evrouteplanner.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Public endpoints for registration and login. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
