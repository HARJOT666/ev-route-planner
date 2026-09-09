package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.TripStop;
import lombok.Data;

/** One charging stop in the returned plan. */
@Data
public class PlannedStopDto {

    private Long stationId;
    private String stationName;
    private double latitude;
    private double longitude;
    private double distanceFromPreviousKm;
    private double batteryArrivalPercent;
    private double batteryDeparturePercent;
    private double energyAddedKwh;
    private double chargingTimeMinutes;
    private double chargingCost;

    public PlannedStopDto() {
    }

    public PlannedStopDto(TripStop stop) {
        this.stationId = stop.getStationId();
        this.stationName = stop.getStationName();
        this.latitude = stop.getLatitude();
        this.longitude = stop.getLongitude();
        this.distanceFromPreviousKm = stop.getDistanceFromPreviousKm();
        this.batteryArrivalPercent = stop.getBatteryArrivalPercent();
        this.batteryDeparturePercent = stop.getBatteryDeparturePercent();
        this.energyAddedKwh = stop.getEnergyAddedKwh();
        this.chargingTimeMinutes = stop.getChargingTimeMinutes();
        this.chargingCost = stop.getChargingCost();
    }
}
