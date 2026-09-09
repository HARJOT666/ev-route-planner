package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.ChargingStation;
import com.example.evrouteplanner.model.StationStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * Charging station data sent to the frontend. Implements Serializable so it can
 * be stored in Redis (the station list is cached).
 */
@Data
public class StationResponse implements Serializable {

    private Long id;
    private String name;
    private double latitude;
    private double longitude;
    private double powerKw;
    private double pricePerKwh;
    private StationStatus status;

    public StationResponse() {
        // needed for JSON/Redis deserialization
    }

    public StationResponse(ChargingStation station) {
        this.id = station.getId();
        this.name = station.getName();
        this.latitude = station.getLatitude();
        this.longitude = station.getLongitude();
        this.powerKw = station.getPowerKw();
        this.pricePerKwh = station.getPricePerKwh();
        this.status = station.getStatus();
    }
}
