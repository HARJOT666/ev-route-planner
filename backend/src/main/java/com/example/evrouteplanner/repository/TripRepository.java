package com.example.evrouteplanner.repository;

import com.example.evrouteplanner.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

    // Newest trips first, only for the given user (trip history is per-user).
    List<Trip> findByUserIdOrderByCreatedAtDesc(Long userId);
}
