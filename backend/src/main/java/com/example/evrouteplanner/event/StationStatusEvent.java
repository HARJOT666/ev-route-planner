package com.example.evrouteplanner.event;

import com.example.evrouteplanner.model.StationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A simple event describing a charging station status change.
 * Published to Kafka whenever a station goes AVAILABLE -> FULL, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationStatusEvent {
    private Long stationId;
    private String stationName;
    private StationStatus oldStatus;
    private StationStatus newStatus;
}
