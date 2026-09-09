package com.example.evrouteplanner.dto;

import com.example.evrouteplanner.model.StationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Input for changing a station's status (AVAILABLE / FULL / OUT_OF_SERVICE). */
@Data
public class StationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private StationStatus status;
}
