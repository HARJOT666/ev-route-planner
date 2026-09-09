package com.example.evrouteplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A station the optimizer looked at but did NOT pick, plus the reason.
 * This is what lets us explain "why station B was chosen instead of A".
 */
@Data
@AllArgsConstructor
public class RejectedStationDto {
    private Long stationId;
    private String stationName;
    private String reason;
}
