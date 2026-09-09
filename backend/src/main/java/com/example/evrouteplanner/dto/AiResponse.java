package com.example.evrouteplanner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** A plain-text answer/explanation from Gemini. */
@Data
@AllArgsConstructor
public class AiResponse {
    private String answer;
}
