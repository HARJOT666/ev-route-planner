package com.example.evrouteplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A saved trip plan for a user. Holds the summary numbers produced by the
 * TripOptimizer plus the ordered list of charging stops.
 */
@Entity
@Table(name = "trips")
@Getter
@Setter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String startName;
    private double startLat;
    private double startLon;

    private String destinationName;
    private double destinationLat;
    private double destinationLon;

    @Enumerated(EnumType.STRING)
    private OptimizationMode mode;

    private String vehicleName;

    // Whether the destination is reachable at all (with or without charging).
    private boolean feasible;

    private double startBatteryPercent;
    private double totalDistanceKm;
    private double drivingTimeMinutes;
    private double chargingTimeMinutes;
    private double totalCost;

    private Instant createdAt = Instant.now();

    // Human-friendly explanation produced by Gemini (may be null until requested).
    @Column(columnDefinition = "TEXT")
    private String aiExplanation;

    // Ordered charging stops. cascade = ALL so stops are saved together with the trip.
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<TripStop> stops = new ArrayList<>();

    public void addStop(TripStop stop) {
        stop.setTrip(this);
        this.stops.add(stop);
    }
}
