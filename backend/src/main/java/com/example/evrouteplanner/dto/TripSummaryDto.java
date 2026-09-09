package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.OptimizationMode;
import com.example.evrouteplanner.model.Trip;
import lombok.Data;

import java.time.Instant;

/** Compact trip info for the history list. */
@Data
public class TripSummaryDto {

    private Long id;
    private String startName;
    private String destinationName;
    private Instant createdAt;
    private OptimizationMode mode;
    private int numberOfStops;
    private double totalDistanceKm;
    private double estimatedDurationMinutes;   // driving + charging
    private boolean feasible;

    public TripSummaryDto(Trip trip) {
        this.id = trip.getId();
        this.startName = trip.getStartName();
        this.destinationName = trip.getDestinationName();
        this.createdAt = trip.getCreatedAt();
        this.mode = trip.getMode();
        this.numberOfStops = trip.getStops().size();
        this.totalDistanceKm = trip.getTotalDistanceKm();
        this.estimatedDurationMinutes = trip.getDrivingTimeMinutes() + trip.getChargingTimeMinutes();
        this.feasible = trip.isFeasible();
    }
}
