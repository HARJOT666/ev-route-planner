package com.example.evrouteplanner.controller;

import com.example.evrouteplanner.dto.AiQuestionRequest;
import com.example.evrouteplanner.dto.AiResponse;
import com.example.evrouteplanner.dto.TripPlanResponse;
import com.example.evrouteplanner.model.Trip;
import com.example.evrouteplanner.service.GeminiService;
import com.example.evrouteplanner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * AI explanation layer. Gemini only explains the plan the optimizer already
 * produced — it never changes the plan. Requires a valid JWT.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final TripService tripService;
    private final GeminiService geminiService;

    public AiController(TripService tripService, GeminiService geminiService) {
        this.tripService = tripService;
        this.geminiService = geminiService;
    }

    // Explain why the saved plan was chosen (and remember the explanation).
    @PostMapping("/trips/{tripId}/explain")
    public AiResponse explainTrip(@PathVariable Long tripId) {
        Trip trip = tripService.loadOwnedTrip(tripId);
        TripPlanResponse plan = tripService.toPlanResponse(trip);

        String explanation = geminiService.explainPlan(plan);
        tripService.saveAiExplanation(trip, explanation);

        return new AiResponse(explanation);
    }

    // Answer a free-text question about the saved plan.
    @PostMapping("/trips/{tripId}/ask")
    public AiResponse askAboutTrip(@PathVariable Long tripId,
                                   @Valid @RequestBody AiQuestionRequest request) {
        Trip trip = tripService.loadOwnedTrip(tripId);
        TripPlanResponse plan = tripService.toPlanResponse(trip);

        String answer = geminiService.answerQuestion(plan, request.getQuestion());
        return new AiResponse(answer);
    }
}
