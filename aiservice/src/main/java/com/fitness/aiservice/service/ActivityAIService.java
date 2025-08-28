package com.fitness.aiservice.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAIService {

    private final GeminiService geminiService;

    public Recommendation generateRecommendation(Activity activity) {
        String prompt = createPromptForActivity(activity);
        String response = geminiService.getAnswer(prompt);
        log.info("Response from AI: {}", response);

        return processAIResponse(activity, response);
    }

    private Recommendation processAIResponse(Activity activity, String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Assuming the response JSON structure matches the Recommendation class
            JsonNode rootNode = objectMapper.readTree(response);

            JsonNode textnode = rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            String jsonContent = textnode.asText()
                    .replaceAll("```json\\n", "")
                    .replaceAll("\\n```", "")
                    .trim();
            JsonNode analysisJson = objectMapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");
            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode, "overall", "Overall: ");
            addAnalysisSection(fullAnalysis, analysisNode, "pace", "Pace: ");
            addAnalysisSection(fullAnalysis, analysisNode, "heartRate", "Heart Rate: ");
            addAnalysisSection(fullAnalysis, analysisNode, "caloriesBurned", "Calories Burned: ");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));
            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));
            List<String> safetyList = extractSafetyTips(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getType())
                    .recommendation(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safetyList)
                    .createdAt(LocalDateTime.now())
                    .build();

        } catch (JsonProcessingException e) {
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendation("Could not generate recommendation due to an error.")
                .improvements(Collections.singletonList("No improvements available"))
                .suggestions(Collections.singletonList("No suggestions available"))
                .safety(Collections.singletonList("Follow general safety guidelines"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private List<String> extractSafetyTips(JsonNode safetyNode) {
        List<String> safetyTips = new ArrayList<>();
        if (safetyNode.isArray()) {
            safetyNode.forEach(safetyTip -> {
                safetyTips.add(safetyTip.asText());
            });
        }
        return safetyTips.isEmpty() ? Collections.singletonList("Follow general safety guidelines") : safetyTips;
    }

    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if (suggestionsNode.isArray()) {
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asText();
                String description = suggestion.path("description").asText();
                suggestions.add(String.format("%s: %s", workout, description));
            });
        }
        return suggestions.isEmpty() ? Collections.singletonList("No suggestions provided") : suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if (improvementsNode.isArray()) {
            improvementsNode.forEach(improvement -> {
                String area = improvement.path("area").asText();
                String recommendation = improvement.path("recommendation").asText();
                improvements.add(String.format("%s: %s", area, recommendation));
            });
        }
        return improvements.isEmpty() ? Collections.singletonList("No improvements provided") : improvements;
    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if (!analysisNode.path(key).isMissingNode()) {
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asText())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
                Analyze this fitness activity and provide detailed recommendations in the following EXACT JSON format:
                {
                    "analysis": {
                        "overall": "Overall analysis here",
                        "pace": "Pace analysis here",
                        "heartRate": "Heart rate analysis here",
                        "caloriesBurned": "Calories analysis here"
                    },
                    "improvements": [
                        {
                            "area": "Area name",
                            "recommendation": "Detailed recommendation"
                        }
                    ],
                    "suggestions": [
                        {
                            "workout": "Workout name",
                            "description": "Detailed workout description"
                        }
                    ],
                    "safety": [
                        "Safety tip 1",
                        "Safety tip 2"
                    ]
                }
                
                Analyse the following activity data:
                Activity Type: %s
                Duration (minutes): %d
                Calories burned: %d
                Additional Metrics: %s

                Provide detailed analysis focusing on performance, improvements, next workout suggestions, and safety tips.
                Ensure the response is strictly in the specified JSON format without any additional text or explanation.
                """,
                activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurnt(),
                activity.getAdditionalMetrics() != null ? activity.getAdditionalMetrics().toString() : "None"
        );
    }
}
