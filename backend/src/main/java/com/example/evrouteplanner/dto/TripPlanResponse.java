package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.OptimizationMode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The full result of planning a trip. This is what the optimizer builds and the
 * controller returns to the frontend. It contains everything needed to draw the
 * map, show the summary cards and feed Gemini structured facts.
 */
@Data
public class TripPlanResponse {

    private Long tripId;              // set after the trip is saved to the database

    private boolean feasible;         // can the destination be reached at all?
    private boolean directReach;      // reachable without any charging stop?
    private String message;           // short human summary of the outcome

    private String startName;
    private double startLat;
    private double startLon;

    private String destinationName;
    private double destinationLat;
    private double destinationLon;

    private OptimizationMode mode;
    private String vehicleName;

    private double startBatteryPercent;
    private double arrivalBatteryPercent;   // battery left at destination

    private double totalDistanceKm;
    private double drivingTimeMinutes;
    private double chargingTimeMinutes;
    private double totalCost;

    private List<PlannedStopDto> stops = new ArrayList<>();
    private List<RejectedStationDto> rejectedStations = new ArrayList<>();

    private String aiExplanation;     // filled in by Gemini when requested
}
