package com.example.evrouteplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Returned after a successful register or login. */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String name;
    private String email;
}
