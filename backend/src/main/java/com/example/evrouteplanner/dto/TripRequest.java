package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.OptimizationMode;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Input for planning a trip. The frontend sends coordinates (picked on the map
 * or geocoded) plus a readable place name for display.
 */
@Data
public class TripRequest {

    @NotBlank(message = "Start location name is required")
    private String startName;
    private double startLat;
    private double startLon;

    @NotBlank(message = "Destination name is required")
    private String destinationName;
    private double destinationLat;
    private double destinationLon;

    @NotNull(message = "Vehicle is required")
    private Long vehicleId;

    @Min(value = 1, message = "Battery percent must be between 1 and 100")
    @Max(value = 100, message = "Battery percent must be between 1 and 100")
    private double batteryPercent;

    @NotNull(message = "Optimization mode is required")
    private OptimizationMode mode;
}
