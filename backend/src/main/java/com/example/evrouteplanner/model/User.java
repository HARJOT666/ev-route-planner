package com.example.evrouteplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * An application user. A user owns vehicles and trips.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    // Stored as a BCrypt hash, never in plain text.
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;
}
