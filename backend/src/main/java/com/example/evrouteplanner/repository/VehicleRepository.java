package com.example.evrouteplanner.repository;

import com.example.evrouteplanner.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByUserId(Long userId);
}
