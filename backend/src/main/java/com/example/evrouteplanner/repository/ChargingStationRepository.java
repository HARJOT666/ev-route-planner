package com.example.evrouteplanner.repository;

import com.example.evrouteplanner.model.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, Long> {
    // findAll(), findById(), save() are inherited from JpaRepository.
}
