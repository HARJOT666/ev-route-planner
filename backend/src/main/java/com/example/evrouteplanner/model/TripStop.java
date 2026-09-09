package com.example.evrouteplanner.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * One charging stop inside a trip. Records where we stopped, how long we
 * charged, how much energy/money it took and the battery level in/out.
 */
@Entity
@Table(name = "trip_stops")
@Getter
@Setter
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    private int stopOrder;        // 1, 2, 3 ... in visiting order

    private Long stationId;
    private String stationName;
    private double latitude;
    private double longitude;

    private double distanceFromPreviousKm;   // driven since previous point
    private double batteryArrivalPercent;    // battery % when we arrive
    private double batteryDeparturePercent;  // battery % when we leave
    private double energyAddedKwh;           // energy charged here
    private double chargingTimeMinutes;
    private double chargingCost;
}
