package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AICategorizationService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    @Value("${huggingface.api.url}")
    private String apiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Categorizes a post using pattern matching and keyword analysis
     * Returns a comma-separated string of categories
     */
    public String categorizePost(String title, String content) {
        try {
            List<String> categories = new ArrayList<>();

            String text = (title + " " + (content != null ? content : "")).toLowerCase();

            // Extract course codes using regex
            Pattern coursePattern = Pattern.compile("\\b([A-Z]{4})\\s*-?\\s*(\\d{3,4})\\b", Pattern.CASE_INSENSITIVE);
            Matcher courseMatcher = coursePattern.matcher(title + " " + (content != null ? content : ""));
            while (courseMatcher.find()) {
                String courseCode = courseMatcher.group(1).toUpperCase() + " " + courseMatcher.group(2);
                if (!categories.contains(courseCode)) {
                    categories.add(courseCode);
                }
            }

            // Determine topic category based on keywords
            if (text.contains("exam") || text.contains("midterm") || text.contains("final") || text.contains("test")) {
                categories.add("exam");
            } else if (text.contains("assignment") || text.contains("homework") || text.contains("hw") || text.contains("due")) {
                categories.add("assignment");
            } else if (text.contains("study") || text.contains("group") || text.contains("study group")) {
                categories.add("study-group");
            } else if (text.contains("project") || text.contains("team")) {
                categories.add("project");
            } else if (text.contains("housing") || text.contains("apartment") || text.contains("roommate") || text.contains("rent")) {
                categories.add("housing");
            } else if (text.contains("job") || text.contains("career") || text.contains("internship") || text.contains("coop") || text.contains("co-op")) {
                categories.add("career");
            } else if (text.contains("event") || text.contains("party") || text.contains("meet") || text.contains("hangout")) {
                categories.add("social");
            } else if (!categories.isEmpty()) {
                // If we found course codes but no topic, add "general"
                categories.add("general");
            } else {
                categories.add("general");
            }

            return categories.isEmpty() ? "general" : String.join(", ", categories);

        } catch (Exception e) {
            System.err.println("Error categorizing post: " + e.getMessage());
            e.printStackTrace();
            return "general";
        }
    }

    private String buildCategorizationPrompt(String title, String content) {
        String text = title;
        if (content != null && !content.isEmpty()) {
            text += " " + content.substring(0, Math.min(content.length(), 300)); // Limit content length
        }

        return String.format(
            "Extract course codes and category from this university post. " +
            "Course codes are like COMP 232, MATH 204, ENCS 282. " +
            "Categories: exam, assignment, study-group, project, housing, social, career, general. " +
            "Post: %s. " +
            "Answer:",
            text
        );
    }

    private String callHuggingFaceAPI(String prompt) throws Exception {
        // Build request payload
        String payload = String.format("{\"inputs\": \"%s\", \"parameters\": {\"max_new_tokens\": 50, \"return_full_text\": false}}",
                escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("HuggingFace API error: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    private String parseCategories(String apiResponse) {
        try {
            // Parse JSON response
            JsonNode jsonNode = objectMapper.readTree(apiResponse);

            String generatedText;
            if (jsonNode.isArray() && jsonNode.size() > 0) {
                generatedText = jsonNode.get(0).get("generated_text").asText();
            } else {
                generatedText = jsonNode.get("generated_text").asText();
            }

            // Clean up the response - extract only course codes and category
            List<String> categories = new ArrayList<>();

            // Extract course codes (e.g., COMP 232, MATH 204)
            Pattern coursePattern = Pattern.compile("\\b([A-Z]{4})\\s*(\\d{3,4})\\b");
            Matcher courseMatcher = coursePattern.matcher(generatedText);
            while (courseMatcher.find()) {
                categories.add(courseMatcher.group(1) + " " + courseMatcher.group(2));
            }

            // Extract topic category
            String[] validTopics = {"exam", "assignment", "study-group", "project", "housing", "social", "career", "general"};
            String lowerResponse = generatedText.toLowerCase();
            for (String topic : validTopics) {
                if (lowerResponse.contains(topic)) {
                    categories.add(topic);
                    break; // Only add one topic
                }
            }

            // If no categories found, return "general"
            if (categories.isEmpty()) {
                return "general";
            }

            return String.join(", ", categories);

        } catch (Exception e) {
            System.err.println("Error parsing AI response: " + e.getMessage());
            return "general";
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
