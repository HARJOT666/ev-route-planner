package com.example.evrouteplanner.exception;

/** Thrown when a requested entity (vehicle, trip, station...) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
