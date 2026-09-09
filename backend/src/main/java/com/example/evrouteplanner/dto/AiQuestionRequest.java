package com.example.evrouteplanner.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** A free-text question the user asks about a saved trip plan. */
@Data
public class AiQuestionRequest {

    @NotBlank(message = "Question is required")
    private String question;
}
