package com.example.evrouteplanner.model;

/**
 * Current availability of a charging station.
 * Only AVAILABLE stations can be used as a charging stop by the optimizer.
 */
public enum StationStatus {
    AVAILABLE,
    FULL,
    OUT_OF_SERVICE
}
