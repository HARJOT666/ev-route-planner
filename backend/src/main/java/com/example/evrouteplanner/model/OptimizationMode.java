package com.example.evrouteplanner.model;

/**
 * How the optimizer should choose between candidate charging stops.
 * FASTEST  -> minimise total trip time (driving + charging).
 * CHEAPEST -> minimise total charging cost.
 * BALANCED -> a simple weighted mix of time and cost.
 */
public enum OptimizationMode {
    FASTEST,
    CHEAPEST,
    BALANCED
}
