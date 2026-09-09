package com.example.evrouteplanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/** Input for creating a vehicle for the logged-in user. */
@Data
public class VehicleRequest {

    @NotBlank(message = "Vehicle name is required")
    private String name;

    @Positive(message = "Battery capacity must be positive")
    private double batteryCapacityKwh;

    @Positive(message = "Efficiency must be positive")
    private double efficiencyKmPerKwh;

    @Positive(message = "Max charging power must be positive")
    private double maxChargingPowerKw;
}
