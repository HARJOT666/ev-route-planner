package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.Vehicle;
import lombok.Data;

/** Vehicle data sent back to the frontend (without the owning user). */
@Data
public class VehicleResponse {

    private Long id;
    private String name;
    private double batteryCapacityKwh;
    private double efficiencyKmPerKwh;
    private double maxChargingPowerKw;

    public VehicleResponse(Vehicle vehicle) {
        this.id = vehicle.getId();
        this.name = vehicle.getName();
        this.batteryCapacityKwh = vehicle.getBatteryCapacityKwh();
        this.efficiencyKmPerKwh = vehicle.getEfficiencyKmPerKwh();
        this.maxChargingPowerKw = vehicle.getMaxChargingPowerKw();
    }
}
