package com.example.evrouteplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * An EV owned by a user. The two numbers that matter for planning are:
 *  - batteryCapacityKwh : total usable battery size in kWh
 *  - efficiencyKmPerKwh : how many km the car drives per kWh
 * Range (km) = batteryCapacityKwh * (battery% / 100) * efficiencyKmPerKwh
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;          // e.g. "Tata Nexon EV"

    @Column(nullable = false)
    private double batteryCapacityKwh;   // e.g. 40.0

    @Column(nullable = false)
    private double efficiencyKmPerKwh;   // e.g. 6.0 (km per kWh)

    // Fastest charging power the car can accept, in kW. Used to estimate charging time.
    @Column(nullable = false)
    private double maxChargingPowerKw;   // e.g. 50.0

    // The vehicle belongs to one user.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
