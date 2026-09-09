package com.example.evrouteplanner.service;

import com.example.evrouteplanner.dto.PlannedStopDto;
import com.example.evrouteplanner.dto.TripPlanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Talks to the Gemini API to produce human-friendly text.
 *
 * IMPORTANT: Gemini never makes optimization decisions. The TripOptimizer has
 * already chosen the route and charging stops. Here we only turn those facts
 * into a readable explanation or answer a user's question about the plan.
 *
 * We build a plain-text "facts" block from the real plan and send it as the
 * prompt, so Gemini can only explain what actually happened.
 */
@Service
public class GeminiService {

    private final String apiKey;
    private final String apiUrl;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiService(@Value("${app.gemini.api-key}") String apiKey,
                         @Value("${app.gemini.url}") String apiUrl) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    /** A short driver-friendly explanation of why this plan was chosen. */
    public String explainPlan(TripPlanResponse plan) {
        String facts = buildFacts(plan);
        String prompt = "You are an assistant for an EV route planner. "
                + "Using ONLY the facts below, explain in 4-6 short sentences why this "
                + "charging plan makes sense for the driver. Mention the optimization mode, "
                + "the charging stops and the battery situation. Do not invent numbers.\n\n"
                + facts;
        return callGemini(prompt);
    }

    /** Answer a user's free-text question about the given plan. */
    public String answerQuestion(TripPlanResponse plan, String question) {
        String facts = buildFacts(plan);
        String prompt = "You are an assistant for an EV route planner. "
                + "Answer the user's question using ONLY the facts below. If the answer is "
                + "not in the facts, say you don't have that information. Keep it short.\n\n"
                + facts + "\nUser question: " + question;
        return callGemini(prompt);
    }

    // Turn the structured plan into a readable facts block for the prompt.
    private String buildFacts(TripPlanResponse plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("Trip facts:\n");
        sb.append("- From: ").append(plan.getStartName()).append("\n");
        sb.append("- To: ").append(plan.getDestinationName()).append("\n");
        sb.append("- Vehicle: ").append(plan.getVehicleName()).append("\n");
        sb.append("- Optimization mode: ").append(plan.getMode()).append("\n");
        sb.append("- Start battery: ").append(plan.getStartBatteryPercent()).append("%\n");
        sb.append("- Reachable: ").append(plan.isFeasible()).append("\n");
        sb.append("- Direct (no charging): ").append(plan.isDirectReach()).append("\n");
        sb.append("- Total distance: ").append(plan.getTotalDistanceKm()).append(" km\n");
        sb.append("- Driving time: ").append(plan.getDrivingTimeMinutes()).append(" minutes\n");
        sb.append("- Charging time: ").append(plan.getChargingTimeMinutes()).append(" minutes\n");
        sb.append("- Total charging cost: ").append(plan.getTotalCost()).append("\n");
        sb.append("- Battery on arrival: ").append(plan.getArrivalBatteryPercent()).append("%\n");

        List<PlannedStopDto> stops = plan.getStops();
        if (stops.isEmpty()) {
            sb.append("- Charging stops: none\n");
        } else {
            sb.append("- Charging stops (in order):\n");
            int i = 1;
            for (PlannedStopDto stop : stops) {
                sb.append("   ").append(i).append(". ").append(stop.getStationName())
                        .append(": arrive at ").append(stop.getBatteryArrivalPercent())
                        .append("%, charge to ").append(stop.getBatteryDeparturePercent())
                        .append("%, ").append(stop.getChargingTimeMinutes()).append(" min, cost ")
                        .append(stop.getChargingCost()).append("\n");
                i++;
            }
        }
        return sb.toString();
    }

    // Makes the actual HTTP call to Gemini and extracts the text answer.
    private String callGemini(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return "AI explanation is unavailable because no Gemini API key is configured. "
                    + "Set the GEMINI_API_KEY environment variable to enable it.";
        }

        try {
            // Request body shape required by the Gemini generateContent API.
            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)))));

            String responseJson = restClient.post()
                    .uri(apiUrl + "?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractText(responseJson);
        } catch (Exception e) {
            return "AI explanation could not be generated right now (" + e.getMessage() + ").";
        }
    }

    // Pull candidates[0].content.parts[0].text out of the Gemini response.
    private String extractText(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode textNode = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text");

        if (textNode.isMissingNode()) {
            return "AI did not return an explanation.";
        }
        return textNode.asText();
    }
}
