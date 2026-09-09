package com.example.evrouteplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A charging station. This is shared system data (not owned by any user).
 * Seeded once at startup with static/mock data.
 */
@Entity
@Table(name = "charging_stations")
@Getter
@Setter
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    // Charging power the station can deliver, in kW (e.g. 50 kW fast charger).
    @Column(nullable = false)
    private double powerKw;

    // Price per kWh of energy, in the local currency (e.g. rupees).
    @Column(nullable = false)
    private double pricePerKwh;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StationStatus status = StationStatus.AVAILABLE;
}
